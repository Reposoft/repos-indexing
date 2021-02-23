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

public class SolrDeleteByQuery extends SolrOp<UpdateResponse> {
	
	private static final Logger logger = LoggerFactory.getLogger(SolrDeleteByQuery.class);
	
	private String query;
	
	public SolrDeleteByQuery(SolrClient core, String query) {
		super(core);
		this.query = query;
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		UpdateResponse delete = core.deleteByQuery(query);
		logger.debug("Delete response: {}", delete);
		doLogSlowQuery("deleteByQuery", query, delete);
		return delete;
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Allowing retry for delete operations. 
		return true;
	}

}
