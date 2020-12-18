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

public class SolrOptimize extends SolrOp<UpdateResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SolrOptimize.class);
	
	public SolrOptimize(SolrClient core) {
		super(core);
	}

	@Override
	protected UpdateResponse runOp() throws SolrServerException, IOException {
		logger.info("Optimizing {}", core);
		return core.optimize(); // TODO use wait flags?
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Allowing retry for optimize operations, likely no risk. 
		return true;
	}
}
