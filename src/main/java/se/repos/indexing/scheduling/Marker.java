/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import se.repos.indexing.item.IndexingItemHandler;

/**
 * Marks scheduling points in handler chain, where all items should have reached the same stage.
 * 
 * Other {@link IndexingItemHandler}s should allow indexing to happen item-by-item for a chain of handlers.
 * 
 * Some of these markers have scheduling significance, such as {@link MarkerOnly} impls.
 */
public interface Marker extends IndexingItemHandler {

	/**
	 * Called after {@link #handle(se.repos.indexing.item.IndexingItemProgress)} of all items, when each one in an indexing unit is at this stage.
	 * 
	 * Note that statefulness from {@link #handle(se.repos.indexing.item.IndexingItemProgress)} is normally safe in a per-repository handler configuration,
	 * because revisions only overlap between handlers and not within them,
	 * but this depends on how {@link IndexingUnit#getHandlers(se.repos.indexing.item.IndexingItemProgress)} is configured.
	 */
	void onItemsMark();
	
}
