/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import java.util.Set;

import se.repos.indexing.item.IndexingItemProgress;

/**
 * Handles one item at a time
 */
public interface IndexingItemHandler {

	public void handle(IndexingItemProgress progress);
	
	/**
	 * Dependencies are used to verify order of execution among handlers,
	 * and also to provide only the expected field data from {@link IndexingDoc#deepCopy()}.
	 * @return other handlers that this one depends on, null for no dependencies
	 */
	public Set<Class<? extends IndexingItemHandler>> getDependencies();
	
}
