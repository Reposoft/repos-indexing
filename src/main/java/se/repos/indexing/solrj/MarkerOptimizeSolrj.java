/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.MarkerWhenIdle;
import se.simonsoft.cms.item.RepoRevision;

public class MarkerOptimizeSolrj implements MarkerWhenIdle {

	public static final int DEFAULT_REVISION_INTERVAL = 1000;
	
	private SolrServer core;
	private int revisionInterval;

	private RepoRevision revision = null;
	
	public MarkerOptimizeSolrj(SolrServer core) {
		this(core, DEFAULT_REVISION_INTERVAL);
	}
	
	public MarkerOptimizeSolrj(SolrServer core, int revisionInterval) {
		this.core = core;
		this.revisionInterval = revisionInterval;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		revision = progress.getRevision();
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void trigger() {
		if (revision.getNumber() % revisionInterval == 0) {
			new SolrOptimize(core).run();
		}		
	}
	
	@Override
	public void ignore() {
	}

}
