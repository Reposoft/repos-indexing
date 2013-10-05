package se.repos.indexing.repository;

import se.repos.indexing.twophases.IndexingItemProgressPhases;

public class IndexingItemHandlerPropertiesDisable extends
		IndexingItemHandlerInternal<IndexingItemProgressPhases> {

	@Override
	public void handleInternal(IndexingItemProgressPhases progress) {
		throw new UnsupportedOperationException("not implemented");
	}

}
