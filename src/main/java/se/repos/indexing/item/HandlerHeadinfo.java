/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.solrj.SolrAdd;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Flags current item as head and previous as non-head.
 * 
 * Stateful singleton because it remembers items that were set to non-head due to isOverwritten, thus saves some updates at multi-revision sync.
 */
@Singleton // 
public class HandlerHeadinfo implements IndexingItemHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	// http://mail-archives.apache.org/mod_mbox/lucene-solr-user/201209.mbox/%3C7E0464726BD046488B66D661770F9C2F01B02EFF0C@TLVMBX01.nice.com%3E
	@SuppressWarnings("serial")
	private final Map<String, Boolean> partialUpdateToFalse = new HashMap<String, Boolean>() {{
		put("set", false);
	}};

	private IdStrategy idStrategy;
	private SolrServer repositem;
	private Map<CmsItemPath, RepoRevision> earlierMarkedOverwritten = new HashMap<CmsItemPath, RepoRevision>();
	
	@Inject
	public HandlerHeadinfo(CmsRepository repository) {
	}
	
	@Inject
	public void setSolrServer(@Named("repositem") SolrServer repositem) {
		this.repositem = repositem;
	}
	
	@Inject
	public void setIdStrategy(IdStrategy idStrategy) {
		this.idStrategy = idStrategy;
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem item = progress.getItem();
		if (item.isFolder()) {
			logger.debug("Skipping head flag on folder because revisionObsoleted is unreliable (see issue in SvnlookItem) and update requires exact id");
			return;
		}
		
		if (item.isAdd()) {
			// do nothing because no earlier revision exists
		} else if (earlierMarkedOverwritten.containsKey(item.getPath())) {
			// item was earlier marked overwritten (head=false), will do nothing but remove the item from earlierMarkedOverwritten.
			// this does not apply to copy operations: never historical copy and only head-copy that is move (should be caught by isAdd() above)
			// #855 The problem is the combination of folder copy (derived items) and updates in same commit, when the copy restores a deleted folder.
			// Not sure why the item is still in earlierMarkedOverwritten. Is the delete operation failing to remove them from that map? Failing to get here?
			RepoRevision itemRevisionObsoleted = item.getRevisionObsoleted();
			if (itemRevisionObsoleted == null) {
				String msg = MessageFormatter.format("Item {} does not report an obsoleted-revision despite being marked overwritten. Combination of copy and modify?", item).getMessage();
				logger.warn(msg);
				// #855 No longer throwing ISE. We should preferably improve the svnlook derive functionality to make these items BOTH isAdd/isCopy and isModified.
			}
			
			RepoRevision obsoleted = earlierMarkedOverwritten.get(item.getPath());
			if (itemRevisionObsoleted != null && !itemRevisionObsoleted.equals(obsoleted)) {
				throw new IllegalStateException("Obsoleted revision " + item.getRevisionObsoleted() + " for " + item + " does not match " + obsoleted + " when it was last indexed");
			}
			earlierMarkedOverwritten.remove(item.getPath());
			logger.trace("Skipping !head flag because {} was known to be later overwritten when indexed at {}", item.getPath(), obsoleted);
		} else {
			indexItemMarkPrevious(progress.getRepository(), progress.getRevision(), item);
		}
		
		if (item.isOverwritten()) {
			progress.getFields().setField("head", false);
			earlierMarkedOverwritten.put(item.getPath(), progress.getRevision());
		} else {
			progress.getFields().setField("head", !item.isDelete());
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	public void indexItemMarkPrevious(CmsRepository repository, RepoRevision revision, CmsChangesetItem item) {
		CmsItemPath path = item.getPath();
		RepoRevision revisionObsoleted = item.getRevisionObsoleted();
		if (revisionObsoleted == null) {
			logger.warn("Unknown obsoleted revision for {}, no existing item will be marked as non-HEAD", item);
			return;
		}
		// If we find a way to update based on query result instead of exact ID we could handle folders as well
		String query = idStrategy.getId(repository, revisionObsoleted, path);
		SolrInputDocument mark = new SolrInputDocument();
		mark.addField("id", query);
		mark.setField("head", partialUpdateToFalse);
		logger.debug("Marking revision {} of {} as non-head since {}, query {}", revisionObsoleted, item.getPath(), revision, query);
		new SolrAdd(repositem, mark).run();
	}

}
