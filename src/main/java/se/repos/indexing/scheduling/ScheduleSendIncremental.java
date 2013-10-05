package se.repos.indexing.scheduling;


import se.repos.indexing.IndexingDocIncremental;

/**
 * Sends all items to index and flags {@link IndexingDocIncremental#setUpdateMode(boolean)} to true.
 */
public interface ScheduleSendIncremental extends Marker {

}
