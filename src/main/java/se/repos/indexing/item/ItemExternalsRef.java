/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.HashSet;
import java.util.Set;

import se.repos.indexing.IndexingItemHandler;

/**
 * Adds entries from svn:externals to "ref" fields.
 */
public class ItemExternalsRef implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		// TODO implement and test for different externals formats
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(HandlerProperties.class);
		}};
	}

}
