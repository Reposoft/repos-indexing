package se.repos.indexing.scheduling;

import se.repos.indexing.item.IndexingItemHandler;

/**
 * Marks scheduling points in handler chain.
 * 
 * Not public because scheduling can only handle pre-defined markers.
 * 
 * TODO needs support for executing an operation once after all items (maybe before all items too)
 */
interface Marker extends IndexingItemHandler {

	
	
}
