package se.repos.indexing.scheduling;

import java.util.concurrent.Semaphore;

import se.repos.indexing.IndexingHandlers;

/**
 * Receives {@link IndexingUnit}s from different repositories and schedules those based on {@link Marker}s.
 * 
 * Global, i.e. normally called by
 * 
 * Common markers are defined in {@link IndexingHandlers}.
 * 
 * 
 */
public interface IndexingSchedule {

	IndexingSchedule start() throws IllegalStateException;
	
	void stop() throws IllegalStateException;
	
	// 
	/**
	 * 
	 * TODO return callback or pass event handler? Actually an extra handler could be a callback.
	 * 
	 * Threaded impls (those supporting {@link ScheduleBackground}) will want to implement this synchronized or with {@link Semaphore}
	 * 
	 * @param unit
	 */
	void add(IndexingUnit unit);
	
	/**
	 * Needed for unit testing.
	 * @return read-only list, current schedule
	 */
	Iterable<IndexingUnit> getQueue();
	
	int getQueueSize();
	
}
