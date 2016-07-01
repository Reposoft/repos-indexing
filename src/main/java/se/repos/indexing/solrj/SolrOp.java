/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared error handling for solr operations.
 */
public abstract class SolrOp implements Runnable {

	protected SolrServer core;
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	public final Long retryPause = 10000L;

	public SolrOp(SolrServer core) {
		this.core = core;
	}

	@Override
	public void run() {
		try {
			runOp();
		} catch (SolrServerException e) {
			// Retry here as well, seems like IOExceptions sometimes are wrapped.
			logger.warn("Solr first attempt failed with SolrServerException, retry in {} ms: {}", retryPause, e.getMessage());
			logger.debug("Solr first attempt failed with SolrServerException: ", e);
			retry();
		} catch (IOException e) {
			logger.warn("Solr first attempt failed with IOException, retry in {} ms: {}", retryPause, e.getMessage());
			logger.debug("Solr first attempt failed with IOException: ", e);
			retry();
		}
	}
	
	private void retry() {

		try {
			Thread.sleep(retryPause);
		} catch (InterruptedException e) {
			throw new RuntimeException("Retry sleep interrupted: " +  e.getMessage());
		}
		
		logger.info("Solr second attempt...");
		try {
			runOp();
		} catch (SolrServerException e) {
			logger.warn("Solr second attempt failed with SolrServerException: ", e);
			throw new RuntimeException("Solr exception during retry", e);
		} catch (IOException e) {
			logger.warn("Solr second attempt failed with IOException: ", e);
			throw new RuntimeException("Solr exception during retry", e);
		}
		logger.info("Solr second attempt successful");
		
	}
	
	protected abstract void runOp() throws SolrServerException, IOException;
	
}
