/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;

/**
 * Encapsulate what varies between schedulers when it comes to {@link Marker} understanding.
 * How to iterate through an {@link IndexingUnit} is defined by the API, not the individual scheduler.
 */
class HandlerIteration {

	private static final Logger logger = LoggerFactory.getLogger(HandlerIteration.class);
	
	private MarkerDecision decision;
	
	public HandlerIteration(MarkerDecision decision) {
		this.decision = decision;
	}
	
	/**
	 * @param unit with the iterator state to start from
	 * @return true if complete
	 */
	public boolean proceed(IndexingUnit unit) {
		while (true) {
			Marker marker = null;
			boolean markerActive = false;
			Iterator<IndexingItemProgress> uit = unit.getItems().iterator();
			while (uit.hasNext()) {
				IndexingItemProgress i = uit.next();
				Iterator<IndexingItemHandler> handlers = unit.getHandlers(i);
				while (handlers.hasNext()) {
					IndexingItemHandler handler = handlers.next();
					if (handler instanceof Marker) {
						if (marker == null) {
							marker = (Marker) handler;
							markerActive = decision.before(marker);
						} else {
							if (!handler.equals(marker)) {
								throw new IllegalArgumentException("Item has a different marker ordering than previous, got " + handler + " for " + i);
							}
						}
						logger.debug("Stopped at marker {} for item {}", marker, i);
						if (markerActive) {
							marker.handle(i);
							break; // no more handlers on this item
						}
					} else {
						if (decision.before(handler, i)) {
							handler.handle(i);
						} else {
							logger.debug("Scheduler {} skipped handler {} for item {}", this, handler, i);
						}
					}
				}
				if (!uit.hasNext() && markerActive) {
					marker.trigger();
					if (!decision.after(marker)) {
						return false;
					}
				}
			}
			if (marker == null) {
				return true;
			}
		}
	}

	public interface MarkerDecision {
		
		/**
		 * Choses to {@link Marker#trigger()} or ignore a {@link Marker#ignore()} a Marker.
		 * 
		 * @return true to run {@link IndexingItemHandler#handle(se.repos.indexing.item.IndexingItemProgress)}
		 */
		boolean before(Marker marker);
		
		/**
		 * Called for handlers, not markers.
		 * 
		 * @return true to execute the handler for the item, false to skip
		 */
		boolean before(IndexingItemHandler handler, IndexingItemProgress item);
		
		/**
		 * If {@link #before(Marker)} returns true this is called after handle items.
		 * If not iteration proceeds beyond this marker.
		 * 
		 * By returning false iteration can be stopped, and the scheduler can later resume iteration at the state remembered by the {@link IndexingUnit}.
		 * 
		 * @return true to continue iteration, false to pause (until next {@link HandlerIteration#proceed(IndexingUnit)})
		 */
		boolean after(Marker marker);
		
	}
	
}
