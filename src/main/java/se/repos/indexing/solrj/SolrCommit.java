/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrCommit extends SolrOp<UpdateResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SolrCommit.class);
	
	public SolrCommit(SolrClient core) {
		super(core);
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		logger.debug("Committing {}", core);
		UpdateResponse response = core.commit();
		doLogSlowQuery("commit", "-", response);
		return response; 
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Retry is a risk for commit, could indicate a SolR restart that can loose pending changes.
		return false;
	}
}
