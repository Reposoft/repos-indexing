/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrOptimize extends SolrOp {

	private static final Logger logger = LoggerFactory.getLogger(SolrOptimize.class);
	
	public SolrOptimize(SolrServer core) {
		super(core);
	}

	@Override
	protected void runOp() throws SolrServerException, IOException {
		logger.info("Optimizing {}", core);
		core.optimize(); // TODO use wait flags?
	}

}
