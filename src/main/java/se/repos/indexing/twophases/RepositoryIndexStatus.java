/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerHeadinfo;
import se.repos.indexing.repository.ReposIndexingPerRepository;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrCommit;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Stuff from the original {@link ReposIndexingImpl} that was needed in {@link ReposIndexingPerRepository}
 * but couldn't be extracted to {@link IndexingItemHandler}s within the current architecture.
 * 
 * Could be made a first class service but then it should also handle caching of results
 * (and get updated based on indexing progress)
 * so that any service could use it to ask for status.
 */
public class RepositoryIndexStatus {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private SolrServer repositem;
	private IdStrategy idStrategy;
	
	@Inject
	public RepositoryIndexStatus(@Named("repositem") SolrServer repositem, IdStrategy idStrategy) {
		this.repositem = repositem;
		this.idStrategy = idStrategy;
	}
	
	protected void solrAdd(SolrInputDocument doc) {
		if (doc.size() == 0) {
			throw new IllegalArgumentException("Detected attempt to index empty document");
		}
		if (doc == IndexingDocIncrementalSolrj.UPDATE_MODE_NO_CHANGES) {
			logger.warn("Index add was attempted in update mode but no changes have been made, got fields {}", doc.getFieldNames());
			return;
		}
		new SolrAdd(repositem, doc).run();
	}

	protected String quote(String fieldValue) {
		return '"' + fieldValue.replace("\"", "\\\"") + '"';
	}
	
	protected String getPropRevKey(String key) {
		return "proprev_".concat(key.replace(':', '.'));
	}

	/**
	 * @return started and completed revision, highest number
	 */
	public RepoRevision getIndexedRevisionHighestCompleted(CmsRepository repository) {
		logger.debug("Checking higest clompleted revision for {}", repository);
		return getIndexedRevision(repository, "true", ORDER.desc);
	}

	/**
	 * @return started but not completed revision, highest number
	 */
	public RepoRevision getIndexedRevisionHighestStarted(CmsRepository repository) {
		return getIndexedRevision(repository, "false", ORDER.desc);
	}

	/**
	 * @return started but not completed revision, lowedst number
	 */
	public RepoRevision getIndexedRevisionLowestStarted(CmsRepository repository) {
		return getIndexedRevision(repository, "false", ORDER.asc);
	}

	private RepoRevision getIndexedRevision(CmsRepository repository, String valComplete, ORDER order) {
		String repoid = idStrategy.getIdRepository(repository);
		logger.debug("Running revision query for {}, complete={}, order={}", repository, valComplete, order);
		SolrQuery query = new SolrQuery("type:commit AND complete:" + valComplete + " AND repoid:" + quote(repoid));
		query.setRows(1);
		query.setFields("rev", "revt");
		query.setSort("rev", order); // the timestamp might be in a different order in svn, if revprops or loading has been used irregularly
		QueryResponse resp;
		try {
			logger.trace("Running revision query {}", query);
			resp = repositem.query(query);
		} catch (SolrServerException e) {
			throw new RuntimeException("Error not handled", e);
		}
		SolrDocumentList results = resp.getResults();
		if (results.getNumFound() == 0) {
			return null;
		}
		SolrDocument r = results.get(0);
		Long rev = (Long) r.getFieldValue("rev");
		if (rev == null) {
			throw new AssertionError("Commit entry lacks rev field: " + r);
		}
		Date revt = (Date) r.getFieldValue("revt");
		if (revt == null) {
			throw new AssertionError("Commit entry lacks revt field: " + r);
		}
		return new RepoRevision(rev, revt);
	}

	/**
	 * No easy way to run this as a handler becuase handlers can't create new solr documents.
	 * @param repository
	 * @param revision
	 * @param item
	 * @deprecated Use {@link HandlerHeadinfo}
	 */
	public void indexItemMarkPrevious(CmsRepository repository, RepoRevision revision, CmsChangesetItem item) {
		if (item.isFolder()) {
			logger.warn("Flagging !head on folder is unreliable, see issue in SvnlookItem");
		}
		CmsItemPath path = item.getPath();
		RepoRevision revisionObsoleted = item.getRevisionObsoleted();
		if (revisionObsoleted == null) {
			logger.warn("Unknown obsoleted revision for {}, no existing item will be marked as non-HEAD", item);
			return;
		}
		String query = repository.getHost() + repository.getUrlAtHost() + (path == null ? "" : path) + "@" + revisionObsoleted.getNumber(); // From ItemPathInfo
		IndexingDocIncrementalSolrj mark = new IndexingDocIncrementalSolrj();
		mark.addField("id", query);		
		mark.setUpdateMode(true);
		mark.setField("head", false);
		this.solrAdd(mark.getSolrDoc());
	}

	/**
	 * @return Commit ID field value
	 */
	protected String indexRevFlag(CmsRepository repository, RepoRevision revision, CmsItemProperties revprops, String error, boolean complete) {

		String id = idStrategy.getIdCommit(repository, revision);
		String repoid = idStrategy.getIdRepository(repository);
		SolrInputDocument docStart = new SolrInputDocument();
		docStart.addField("id", id);
		docStart.addField("repo", repository.getName());
		docStart.addField("repoid", repoid);
		docStart.addField("repoparent", repository.getParentPath());
		docStart.addField("repohost", repository.getHost());
		
		docStart.addField("type", "commit");
		docStart.addField("rev", idStrategy.getIdRevision(revision));
		docStart.addField("revt", revision.getDate());
		docStart.addField("complete", complete);
		docStart.addField("t", new Date());
		
		if (revprops != null) {
			for (String key : revprops.getKeySet()) {
				docStart.addField(getPropRevKey(key), revprops.getString(key));
			}
		}

		if (error != null) {
			docStart.addField("text_error", error);
		}
		
		this.solrAdd(docStart);
		return id;
	}

	public String indexRevStart(CmsRepository repository, RepoRevision revision, CmsItemProperties revprops, String error) {
		return indexRevFlag(repository, revision, revprops, error, false);
	}
	
	public String indexRevEmpty(CmsRepository repository,
			RepoRevision revision, CmsItemProperties revprops, String error) {
		return indexRevFlag(repository, revision, revprops, error, true);
	}
	
	public void indexRevStartAndCommit(CmsRepository repository,
			RepoRevision revision, CmsItemProperties revprops) {
		indexRevStart(repository, revision, revprops, null);
		new SolrCommit(repositem).run();
	}
	
	@Deprecated // has been moved to MarkerRevisionComplete
	public void indexRevComplete(String id) {
		SolrInputDocument docComplete = new SolrInputDocument();
		docComplete.addField("id", id);
		docComplete.setField("complete", partialUpdateToTrue);
		this.solrAdd(docComplete);
	}
	
	// http://mail-archives.apache.org/mod_mbox/lucene-solr-user/201209.mbox/%3C7E0464726BD046488B66D661770F9C2F01B02EFF0C@TLVMBX01.nice.com%3E
	@SuppressWarnings("serial")
	final Map<String, Boolean> partialUpdateToTrue = new HashMap<String, Boolean>() {{
		put("set", true);
	}};

}