/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Shared error handling for solr operations.
 */
public abstract class SolrOp implements Runnable {

	protected SolrServer core;

	public SolrOp(SolrServer core) {
		this.core = core;
	}

	@Override
	public void run() {
		try {
			runOp();
		} catch (SolrServerException e) {
			throw new RuntimeException("Solr error not handled", e);
		} catch (IOException e) {
			// retry?
			throw new RuntimeException("Solr error not handled", e);
		}
	}
	
	public abstract void runOp() throws SolrServerException, IOException;
	
}
