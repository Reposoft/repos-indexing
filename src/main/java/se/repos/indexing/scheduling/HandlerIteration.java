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
	
	public HandlerIteration(MarkerDecision decision) {
		
	}
	
	public void run(IndexingUnit unit) {
		// TODO straight from BlockingOnly, meeds to be adapted to fulfil the contract of MarkerDecision
		Marker marker = null;
		while (true) {
			for (IndexingItemProgress i : unit.getItems()) {
				Iterator<IndexingItemHandler> handlers = unit.getHandlers(i);
				while (handlers.hasNext()) {
					IndexingItemHandler handler = handlers.next();
					handler.handle(i);
					if (handler instanceof Marker) {
						if (marker == null) {
							marker = (Marker) handler;
						} else {
							if (!handler.equals(marker)) {
								throw new IllegalArgumentException("Item has a different marker ordering than previous, got " + handler + " for " + i);
							}
						}
						logger.trace("Stopped at marker {} for item {}", marker, i);
						break;
					}
				}
			}
			if (marker == null) {
				break;
			} else {
				marker.trigger();
				marker = null;
			}
		}		
	}

	public interface MarkerDecision {
		
		/**
		 * Choses to {@link Marker#trigger()} or ignore a {@link Marker#ignore()} a Marker.
		 * @return true to run {@link IndexingItemHandler#handle(se.repos.indexing.item.IndexingItemProgress)}
		 */
		boolean before(Marker marker);
		
		/**
		 * If {@link #before(Marker)} returns true this is called after handle items.
		 */
		void after(Marker marker);
		
	}
	
}
