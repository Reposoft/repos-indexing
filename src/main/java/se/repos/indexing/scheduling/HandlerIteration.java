/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingHandlerException;
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
		java.util.Collection<IndexingItemHandler> seen = logger.isTraceEnabled() ? new java.util.HashSet<IndexingItemHandler>() : null;
		while (true) {
			Marker marker = null;
			Collection<Marker> skip = new LinkedList<Marker>();
			Iterator<IndexingItemProgress> uit = unit.getItems().iterator();
			while (uit.hasNext()) {
				IndexingItemProgress i = uit.next();
				Iterator<IndexingItemHandler> handlers = unit.getHandlers(i);
				while (handlers.hasNext()) {
					try {
						IndexingItemHandler handler = handlers.next();
						if (logger.isTraceEnabled()) {
							if (!seen.contains(handler)) {
								seen.add(handler);
								logger.trace("Running {} {} for the first time this unit", handler instanceof Marker ? "marker" : "handler", handler);
							}
							if (!uit.hasNext()) {
								logger.trace("Running {} {} for the last time this unit", handler instanceof Marker ? "marker" : "handler", handler);
							}
						}
						if (handler instanceof java.lang.reflect.Proxy) {
							logger.warn("Handler {} is a dynamic proxy. Even if the proxied class is a Marker it can't be handled as such.", handler); // can't cast, right?
						}
						if (handler instanceof Marker) {
							Marker m = (Marker) handler;
							if (marker == null) {
								if (decision.before(m)) {
									marker = m;
								} else {
									logger.trace("Skip marker {} for this indexing unit", m);
									skip.add(m);
									m.ignore();
								}
							}
							if (skip.contains(m)) {
								logger.trace("Skip marker {} as decided for first item", m);
							} else if (m.equals(marker)) {
								logger.trace("Handling marker {}", m);
								m.handle(i);
								if (!uit.hasNext()) {
									logger.trace("Trigger marker {}, all items level", m);
									marker.trigger();
									if (!decision.after(marker)) {
										return false;
									}
								}
								break;
							} else {
								throw new IllegalArgumentException("Item has a different marker ordering than previous, got " + handler + " for " + i);
							}
						} else {
							if (decision.before(handler, i)) {
								handler.handle(i);
							} else {
								logger.trace("Skip handler {} for item {} as decided by {}", handler, i, decision);
							}
						}
					} catch (IndexingHandlerException ex) {
						logger.warn(ex.getMessage());
						StringWriter stackTraceWriter = new StringWriter();
						ex.printStackTrace(new PrintWriter(stackTraceWriter));
						i.getFields().addField("text_error", stackTraceWriter.toString());
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
