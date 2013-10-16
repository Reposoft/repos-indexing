package se.repos.indexing;

import com.google.inject.Singleton;

/**
 * Administrative tasks on index.
 */
@Singleton // Notification receivers won't be notified if this isn't a singleton
public interface IndexAdmin {

	/**
	 * Adds event handler for the administrative tasks, to be called after the corresponsing operations.
	 * @param notificationHandler on which {@link #addPostAction(IndexAdmin)} is not allowed.
	 */
	void addPostAction(IndexAdmin notificationReceiver);
	
	/**
	 * Delete all entries for the current context (usually a single repository) and commit.
	 */
	void clear();
	
}
