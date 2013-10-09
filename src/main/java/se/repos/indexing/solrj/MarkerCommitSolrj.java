/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;

public class MarkerCommitSolrj implements Marker {

	private SolrServer core;

	public MarkerCommitSolrj(SolrServer core) {
		this.core = core;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void trigger() {
		new SolrCommit(core).run();
	}
	
	@Override
	public void ignore() {
	}	

}
