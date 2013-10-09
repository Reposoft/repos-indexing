/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import java.util.Set;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;

/**
 * Marker that does nothing except signal to scheduling.
 */
abstract class MarkerOnly implements Marker {

	@Override
	public void handle(IndexingItemProgress progress) {
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void trigger() {
	}

	@Override
	public void ignore() {
	}
	
}
