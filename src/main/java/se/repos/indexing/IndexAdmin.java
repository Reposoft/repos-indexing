/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

/**
 * Administrative tasks on index.
 */
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
