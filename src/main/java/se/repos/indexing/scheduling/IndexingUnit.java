package se.repos.indexing.scheduling;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

import se.repos.indexing.IndexingEventAware;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

/**
 * Responsible for iterating through items and handlers and pause at specific {@link Marker}s.
 * 
 * Keeps track of handlers so that each is invoked once per item.
 * 
 * The is one fundamental complexity with indexing.
 *  - Should run one item at a time to keep the number of content caches down. (running one handler at a time for large commits is impractical).
 *  - Each item is visited by a number of handlers, but some of these are cross-item actions such as incremental send and place the rest in background.
 *  
 * {@link IndexingEventAware} must be notified when all handlers have been completed for all items.
 * 
 * Status reporting needs a bit more info, maybe cross-item handlers are good notification points.
 * 
 * @author solsson
 *
 */
class IndexingUnit {

	// iteration order must be same as original
	Map<Marker, List<IndexingItemHandler>> phases = new LinkedHashMap<Marker, List<IndexingItemHandler>>();
	
	/**
	 * @param items Passed through to handlers in an order decided by {@link IndexingSchedule}
	 * @param handler Ordered list for indexing execution. Handlers are not necessarily unique.
	 */
	public IndexingUnit(Iterable<IndexingItemProgress> items, Iterable<IndexingItemHandler> handler) {
		
	}
	
	/**
	 * The actual iteration of a changeset, for example, might be deferred for memory reasons.
	 * @return
	 */
	//NavigableSet<IndexingItemProgress> getItem() {
	Iterable<IndexingItemProgress> getItems() {
		return null;
	}
	
	/**
	 * @return TODO stateful iterator per item?
	 */
	Iterator<IndexingItemHandler> getHandlers(IndexingItemProgress item) {
		return null;
	}
	
	boolean hasHandler(IndexingItemHandler handler) {
		return false;
	}
	
	/**
	 * Keeps all items but splits on a cross-item handler and returns all preceding handlers.
	 * @param handler To be removed, along with all handlers after
	 * @return with current as
	IndexingUnit getBefore(Marker handler) {
		return null;
	}
	
	IndexingUnit getAfter(Marker handler) {
		return null;
	}
	
	IndexingUnit getParent() {
		return null;
	}
	 */	

//	
//	static class Handlers implements NavigableMap<Marker, List<IndexingItemHandler>> {
//		
//	}
	
}
