/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import se.repos.indexing.twophases.IndexingItemProgressPhases;

public class HandlerContentDisable extends IndexingItemHandlerInternal<IndexingItemProgressPhases> {

	@Override
	public void handleInternal(IndexingItemProgressPhases progress) {
		progress.getContentBuffer().destroy();
		progress.setContents(null);
	}


}
