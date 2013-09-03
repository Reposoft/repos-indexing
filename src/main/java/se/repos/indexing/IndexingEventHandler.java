/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

public interface IndexingEventHandler {

	void onDocAdd(int batchSize);
	
}
