package se.repos.indexing.repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.Marker;
import se.repos.indexing.solrj.SolrAdd;

public class MarkerRevisionComplete implements Marker {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	// http://mail-archives.apache.org/mod_mbox/lucene-solr-user/201209.mbox/%3C7E0464726BD046488B66D661770F9C2F01B02EFF0C@TLVMBX01.nice.com%3E
	@SuppressWarnings("serial")
	private final Map<String, Boolean> partialUpdateToTrue = new HashMap<String, Boolean>() {{
		put("set", true);
	}};
	
	private SolrServer repositem;

	private String commitIdCurrent = null;
	
	@Inject
	public MarkerRevisionComplete(@Named("repositem") SolrServer repositem) {
		this.repositem = repositem;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		String commitId = (String) progress.getFields().getFieldValue("revid");
		if (commitIdCurrent == null) {
			commitIdCurrent = commitId;
		} else {
			if (!commitIdCurrent.equals(commitId)) {
				throw new IllegalStateException("Revision overlap at " + commitIdCurrent + " and " + commitId + ". Not supported until pre-revision handler support is added.");
			}
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void onItemsMark() {
		if (commitIdCurrent == null) {
			logger.info("Revision was empty");
			return;
		}
		commitIdCurrent = null;
		SolrInputDocument doc = new SolrInputDocument();
		doc.setField("id", commitIdCurrent);
		doc.setField("complete", partialUpdateToTrue);
		new SolrAdd(repositem, doc).run();
	}

}
