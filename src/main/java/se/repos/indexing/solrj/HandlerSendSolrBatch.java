/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.util.Set;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.Marker;

/**
 * Idea: do batch add to solr to improve performance.
 * 
 * Must run as {@link Marker} because any remaining pending docs must be sent after item iteration.
 * 
 * The problem with this is that sending is not done immediately, so memory consumption will build up.
 * For using the index this won't matter in a standard commit scenario, because content is not searchable until after commit anyway.
 * 
 * A different strategy would be to add APIs for adding indexing "docs" from handlers to scheduling,
 * possibly for different cores, and implement central handling that should deprecate {@link SolrAdd}.
 */
public class HandlerSendSolrBatch implements Marker {

	@Override
	public void handle(IndexingItemProgress progress) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void onItemsMark() {
		throw new UnsupportedOperationException("not implemented");
	}

}
