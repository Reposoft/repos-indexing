/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.Date;
import java.util.Set;

import se.repos.indexing.IndexingItemHandler;

public class HandlerIndexTime implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		progress.getFields().addField("t", new Date());
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

}
