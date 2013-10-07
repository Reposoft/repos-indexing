package se.repos.indexing.scheduling;

import se.repos.indexing.item.IndexingItemHandler;

/**
 * Marks scheduling points in handler chain, where all items should have reached the same stage.
 * 
 * Other {@link IndexingItemHandler}s should allow indexing to happen item-by-item for a chain of handlers.
 * 
 * Some of these markers have scheduling significance, such as {@link ScheduleBackground} etc.
 */
public interface Marker extends IndexingItemHandler {

	/**
	 * Called after {@link #handle(se.repos.indexing.item.IndexingItemProgress)} of all items, when each one in an indexing unit is at this stage.
	 */
	void onItemsMark();
	
}
