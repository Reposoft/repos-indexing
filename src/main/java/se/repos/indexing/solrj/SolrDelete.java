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

public class SolrDelete extends SolrOp<UpdateResponse> {
	
	private static final Logger logger = LoggerFactory.getLogger(SolrDelete.class);
	
	private String id;
	
	public SolrDelete(SolrClient core, String id) {
		super(core);
		this.id = id;
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		UpdateResponse delete = core.deleteById(id);
		logger.debug("Delete response: {}", delete);
		return delete;
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Allowing retry for delete operations. 
		return true;
	}

}
