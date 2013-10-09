/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrCommit extends SolrOp {

	private static final Logger logger = LoggerFactory.getLogger(SolrCommit.class);
	
	public SolrCommit(SolrServer core) {
		super(core);
	}

	@Override
	public void runOp() throws SolrServerException, IOException {
		logger.debug("Committing {}", core);
		core.commit();
	}

}
