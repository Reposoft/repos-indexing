package se.repos.indexing.scheduling;

/**
 * Allow later added indexing units to proceed up to the same handler (this one) before proceeding.
 * 
 * Normally preceded in handler iteration by {@link ScheduleSendIncremental}.
 */
public interface ScheduleAwaitNewer extends Marker {
	

}
