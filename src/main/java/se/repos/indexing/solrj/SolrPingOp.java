/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrPingOp extends SolrOp<SolrPingResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SolrPingOp.class);
	
	public SolrPingOp(SolrClient core) {
		super(core);
	}

	@Override
	public SolrPingResponse runOp() throws SolrServerException, IOException {
		SolrPingResponse ping = core.ping();
		logger.info("Solr ping: qtime={} time={}", ping.getQTime(), ping.getElapsedTime());
		return ping;
	}

}
