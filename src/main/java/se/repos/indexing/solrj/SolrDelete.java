/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrDelete extends SolrOp {
	
	private static final Logger logger = LoggerFactory.getLogger(SolrDelete.class);
	
	private String query;
	
	public SolrDelete(SolrServer core, String query) {
		super(core);
		this.query = query;
	}

	@Override
	public void runOp() throws SolrServerException, IOException {
		UpdateResponse delete = core.deleteByQuery(query);
		logger.debug("Delete response: {}", delete);
	}

}
