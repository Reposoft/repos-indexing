package se.repos.indexing.scheduling;

import se.repos.indexing.IndexingEventAware;

/**
 * Maps {@link Marker} events to scheduling-agnostic {@link IndexingEventAware}s.
 * 
 * @deprecated Use callback or handler in {@link IndexingSchedule#add(IndexingUnit)} instead.
 */
public abstract class SchedulingAwareIndexingEventAware implements SchedulingAware {

	private IndexingEventAware[] eventAware;

	public SchedulingAwareIndexingEventAware(IndexingEventAware... eventAware) {
		this.eventAware = eventAware;
	}

	@Override
	public void onCompleted(Marker marker) {
		if (marker instanceof ScheduleSendComplete) {
			for (IndexingEventAware a : eventAware) {
				a.onRevisionComplete(null); // TODO get Repository and Revision
			}
		}
	}

}
