/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingEventAware;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentsBuffer;
import se.repos.indexing.item.ItemContentsBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public class ReposIndexingEventsTest {

	private Map<IndexingEventAware, Long> revisions = new HashMap<IndexingEventAware, Long>();
	
	@Test
	public void testOnRevisionComplete() {
		ReposIndexingImpl impl = new ReposIndexingImpl() {
		};
		
		ItemContentsBufferStrategy bufferAware = new AwareBuffer();
		impl.setItemContentsBufferStrategy(bufferAware);
		
		ItemPropertiesBufferStrategy propertiesAware = new AwareProperties();
		impl.setItemPropertiesBufferStrategy(propertiesAware);
		
		Set<IndexingItemHandler> blocking = new LinkedHashSet<IndexingItemHandler>();
		blocking.add(new AwareHandler1());
		impl.setItemBlocking(blocking);
		
		Set<IndexingItemHandler> background = new LinkedHashSet<IndexingItemHandler>();
		background.add(new AwareHandler2());
		impl.setItemBackground(background);
		
		Set<IndexingEventAware> other = new LinkedHashSet<IndexingEventAware>();
		other.add(new AwareArbitrary());
		impl.addEventListeners(other);
		
		Collection<IndexingEventAware> aware = impl.getListerners();
		assertTrue("should add content buffer", aware.contains(bufferAware));
		assertTrue("should add props buffer", aware.contains(propertiesAware));
		assertTrue("should add aware handlers", aware.contains(blocking.iterator().next()));
		assertTrue("should add aware background handlers", aware.contains(background.iterator().next()));
		assertTrue("should add dedicated handlers", aware.contains(other.iterator().next()));
	}
	
	abstract class Aware implements IndexingEventAware {
		
		@Override
		public void onRevisionComplete(RepoRevision revision) {
			revisions.put(this, revision.getNumber());
		}
		
	}
	
	class AwareBuffer extends Aware implements ItemContentsBufferStrategy {

		@Override
		public ItemContentsBuffer getBuffer(CmsRepositoryInspection repository,
				RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
			return null;
		}
		
	}

	class AwareProperties extends Aware implements ItemPropertiesBufferStrategy {

		@Override
		public CmsItemProperties getProperties(
				CmsRepositoryInspection repository, RepoRevision revision,
				CmsItemPath path) {
			return null;
		}
		
	}
	
	class AwareHandler1 extends Aware implements IndexingItemHandler {

		@Override
		public void handle(IndexingItemProgress progress) {
		}

		@Override
		public Set<Class<? extends IndexingItemHandler>> getDependencies() {
			return null;
		}
		
	}
	
	class AwareHandler2 extends Aware implements IndexingItemHandler {

		@Override
		public void handle(IndexingItemProgress progress) {
		}

		@Override
		public Set<Class<? extends IndexingItemHandler>> getDependencies() {
			return null;
		}
		
	}

	class AwareArbitrary extends Aware {
		
	}
	
}
