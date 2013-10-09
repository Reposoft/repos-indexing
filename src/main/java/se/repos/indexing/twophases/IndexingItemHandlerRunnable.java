/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class IndexingItemHandlerRunnable implements Runnable {

	private IndexingItemHandler handler;
	private IndexingItemProgress progress;

	public IndexingItemHandlerRunnable(IndexingItemHandler handler,
			IndexingItemProgress progress) {
		this.handler = handler;
		this.progress = progress;
	}

	@Override
	public void run() {
		handler.handle(progress);
	}

}
