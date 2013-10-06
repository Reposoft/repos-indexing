package se.repos.indexing.repository;

import se.repos.indexing.twophases.IndexingItemProgressPhases;

public class IndexingItemHandlerContentDisable extends IndexingItemHandlerInternal<IndexingItemProgressPhases> {

	@Override
	public void handleInternal(IndexingItemProgressPhases progress) {
		progress.getContentBuffer().destroy();
		progress.setContents(null);
	}


}
