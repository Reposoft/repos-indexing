/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrQueryOp extends SolrOp<QueryResponse> {
	
	private static final Logger logger = LoggerFactory.getLogger(SolrQuery.class);
	
	private SolrQuery query;
	
	public SolrQueryOp(SolrClient core, SolrQuery query) {
		super(core);
		this.query = query;
	}

	@Override
	public QueryResponse runOp() throws SolrServerException, IOException {
		QueryResponse resp = core.query(this.query);
		logger.debug("Query response: {}", query);
		return resp;
	}

}
