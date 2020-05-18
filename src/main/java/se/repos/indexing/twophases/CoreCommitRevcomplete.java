/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

import se.repos.indexing.CoreCommit;
import se.repos.indexing.IndexConnectException;
import se.repos.indexing.IndexWriteException;

/**
 * Given a Solr(j) core, commits after each revision. Never optimizes.
 */
public class CoreCommitRevcomplete implements CoreCommit {

	private SolrClient core;

	public CoreCommitRevcomplete(SolrClient core) {
		this.core = core;
	}
	
	@Override
	public void commit() {
		try {
			core.commit();
		} catch (SolrServerException e) {
			throw new IndexWriteException(e);
		} catch (IOException e) {
			throw new IndexConnectException(e);
		}
	}

	@Override
	public void optimize() {
		try {
			core.optimize();
		} catch (SolrServerException e) {
			throw new IndexWriteException(e);
		} catch (IOException e) {
			throw new IndexConnectException(e);
		}
	}

	@Override
	public void onDocAdd(int batchSize) {
		// NOP
	}

	@Override
	public void onCompleteRevision() {
		commit();
	}

	@Override
	public void onCompleteSync() {
		// NOP
	}
	
}
