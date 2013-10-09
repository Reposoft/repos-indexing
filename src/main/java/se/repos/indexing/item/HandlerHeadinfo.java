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

import se.repos.indexing.IdStrategy;
import se.repos.indexing.solrj.SolrAdd;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

@Singleton // saves some updates at multi-revision sync by remembering items that were set to non-head due to isOverwritten
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
		
		if (item.isDelete()) {
			progress.getFields().setField("head", false);
			return;
		}
		
		if (item.isAdd()) {
			// do nothing because no earlier revision exists
		} else if (earlierMarkedOverwritten.containsKey(item.getPath())) {
			RepoRevision obsoleted = earlierMarkedOverwritten.get(item.getPath());
			if (!item.getRevisionObsoleted().equals(obsoleted)) {
				throw new IllegalStateException("Obsoleted revision " + item.getRevisionObsoleted() + " for " + item + " does not match " + obsoleted + "when it was last indexed");
			}
			earlierMarkedOverwritten.remove(item.getPath());
			logger.debug("Skipping !head flag because {} was known to be later overwritten when indexed at {}", item.getPath(), obsoleted);
		} else {
			indexItemMarkPrevious(progress.getRepository(), progress.getRevision(), item);
		}
		
		if (item.isOverwritten()) {
			progress.getFields().setField("head", false);
			earlierMarkedOverwritten.put(item.getPath(), progress.getRevision());
		} else {
			progress.getFields().setField("head", true);
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
