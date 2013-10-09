package se.repos.indexing.twophases;

import java.io.IOException;
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

import com.google.inject.name.Named;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexConnectException;
import se.repos.indexing.IndexWriteException;
import se.repos.indexing.item.HandlerHeadinfo;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.repository.ReposIndexingPerRepository;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
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
		try {
			repositem.add(doc);
		} catch (SolrServerException e) {
			throw new IndexWriteException(e);
		} catch (IOException e) {
			throw new IndexConnectException(e);
		}
	}

	protected String escape(String fieldValue) {
		return fieldValue.replaceAll("([:^\\(\\)!~/ ])", "\\\\$1");
	}

	protected String quote(String fieldValue) {
		return '"' + fieldValue.replace("\"", "\\\"") + '"';
	}

	public RepoRevision getIndexedRevisionHighestCompleted(CmsRepository repository) {
		logger.debug("Checking higest clompleted revision for {}", repository);
		return getIndexedRevision(repository, "true", ORDER.desc);
	}

	public RepoRevision getIndexedRevisionHighestStarted(CmsRepository repository) {
		return getIndexedRevision(repository, "false", ORDER.desc);
	}

	public RepoRevision getIndexedRevisionLowestStarted(CmsRepository repository) {
		return getIndexedRevision(repository, "false", ORDER.asc);
	}

	private RepoRevision getIndexedRevision(CmsRepository repository, String valComplete, ORDER order) {
		logger.debug("Running revision query for {}, complete={}, order={}", repository, valComplete, order);
		String idPrefix = idStrategy.getIdRepository(repository);
		String idPrefixEscaped = escape(idPrefix);
		logger.debug("Repository's ID prefix is {} ({})", idPrefix, idPrefixEscaped);
		SolrQuery query = new SolrQuery("type:commit AND complete:" + valComplete + " AND id:" + idPrefixEscaped + "*");
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
		Date revt = (Date) r.getFieldValue("revt");
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
	 * 
	 * @param repository
	 * @param revision
	 * @param revprops
	 * @return Commit ID field value
	 */
	public String indexRevStart(CmsRepository repository, RepoRevision revision, CmsItemProperties revprops) {
		String id = idStrategy.getIdCommit(repository, revision);
		SolrInputDocument docStart = new SolrInputDocument();
		docStart.addField("id", id);
		docStart.addField("type", "commit");
		docStart.addField("rev", idStrategy.getIdRevision(revision));
		docStart.addField("complete", false);
		this.solrAdd(docStart);
		return id;
	}

	// has been moved to MarkerRevisionComplete
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