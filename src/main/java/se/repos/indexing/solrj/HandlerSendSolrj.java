/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class HandlerSendSolrj implements IndexingItemHandler {

	private SolrServer core;

	public HandlerSendSolrj(SolrServer core) {
		this.core = core;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		new SolrAdd(core, progress.getFields()).run();
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

}
