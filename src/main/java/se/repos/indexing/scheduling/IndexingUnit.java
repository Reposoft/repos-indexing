/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;

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

	private Map<IndexingItemProgress, Iterator<IndexingItemHandler>> handlers = new LinkedHashMap<IndexingItemProgress, Iterator<IndexingItemHandler>>();
	private Set<Class<? extends IndexingItemHandler>> handlertypes = new LinkedHashSet<Class<? extends IndexingItemHandler>>();
	
	/**
	 * @param items Passed through to handlers in an order decided by {@link IndexingSchedule}
	 * @param handler Ordered list for indexing execution. Handlers are not necessarily unique.
	 */
	public IndexingUnit(Iterable<IndexingItemProgress> items, Iterable<IndexingItemHandler> handler) {
		for (IndexingItemProgress i : items) {
			handlers.put(i, handler.iterator());
		}
	}
	
	/**
	 * The actual iteration of a changeset, for example, might be deferred for memory reasons.
	 * @return
	 */
	Iterable<IndexingItemProgress> getItems() {
		return handlers.keySet();
	}
	
	/**
	 * @return TODO stateful iterator per item?
	 */
	Iterator<IndexingItemHandler> getHandlers(IndexingItemProgress item) {
		return handlers.get(item);
	}
	
	boolean hasHandler(Class<? extends IndexingItemHandler> handler) {
		return handlertypes.contains(handler);
	}
	
}
