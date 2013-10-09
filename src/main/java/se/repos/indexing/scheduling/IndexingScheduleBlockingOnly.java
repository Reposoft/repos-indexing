package se.repos.indexing.scheduling;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

/**
 * Ignores all markers and runs a standard indexing loop, each item in turn processed with all handlers.
 * Good for unit testing because no threads are needed but a standard handler chain can be used.
 */
@Singleton
public class IndexingScheduleBlockingOnly implements IndexingSchedule {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private boolean running = false;
	
	/**
	 * Blocking is always started, no queue.
	 */
	@Override
	public IndexingSchedule start() throws IllegalStateException {
		if (running) {
			throw new IllegalStateException("Indexing already running");
		}
		running = true;
		logger.debug("Indexing is now running");
		return this;
	}

	@Override
	public void stop() throws IllegalStateException {
		if (!running) {
			throw new IllegalStateException("Indexing not running");
		}		
		running = false;
		logger.debug("Indexing is now stopped");
	}

	@Override
	public void add(IndexingUnit unit) {
		if (!running) {
			throw new IllegalStateException("Blocking schedule can only receive indexing units when started");
		}
		run(unit);
	}

	public void run(IndexingUnit unit) {
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
				marker.onItemsMark();
				marker = null;
			}
		}
	} 
	
	@Override
	public Iterable<IndexingUnit> getQueue() {
		return Collections.emptyList();
	}

	@Override
	public int getQueueSize() {
		return 0;
	}
	
	
}
