/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import java.util.Set;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

/**
 * Can make assumptions about the type of {@link IndexingItemProgress}.
 */
public abstract class IndexingItemHandlerInternal<T extends IndexingItemProgress> implements IndexingItemHandler {
	
	@SuppressWarnings("unchecked")
	@Override
	public void handle(IndexingItemProgress progress) {
		try {
			handleInternal((T) progress);
		} catch (ClassCastException e) {
			throw new IllegalStateException("Configuration error. Internal handler " + this.getClass() + " not compatible with indexing progress type " + progress, e);
		}
	}

	/**
	 * Normally internal handlers have no dependencies.
	 */
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	public abstract void handleInternal(T progress);
	
}
