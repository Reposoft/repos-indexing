package se.repos.indexing.repository;

import javax.inject.Inject;

import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.twophases.IndexingItemProgressPhases;

public class IndexingItemHandlerPropertiesEnable extends IndexingItemHandlerInternal<IndexingItemProgressPhases> {

	private ItemPropertiesBufferStrategy strategy;

	@Inject
	public IndexingItemHandlerPropertiesEnable(ItemPropertiesBufferStrategy strategy) {
		this.strategy = strategy;
	}
	
	@Override
	public void handleInternal(IndexingItemProgressPhases progress) {
		progress.setProperties(strategy.getProperties(null, progress.getRevision(), progress.getItem().getPath()));
	}

}
