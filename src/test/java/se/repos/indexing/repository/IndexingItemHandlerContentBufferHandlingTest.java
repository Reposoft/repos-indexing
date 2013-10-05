package se.repos.indexing.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.Test;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.twophases.IndexingItemProgressPhases;

public class IndexingItemHandlerContentBufferHandlingTest {

	@Test
	public void test() {
		ItemContentBufferStrategy strategy = mock(ItemContentBufferStrategy.class);
		IndexingItemHandler enable = new IndexingItemHandlerContentEnable(strategy);
		IndexingItemHandler disable = new IndexingItemHandlerContentDisable();
		
		IndexingItemProgress progress = new IndexingItemProgressPhases(null,null,null,null); //repository, revision, item, fields);

		try {
			progress.getContents();
		} catch (IllegalStateException e) {
			// expected
		}
		
		enable.handle(progress);
		
	}

	/**
	 * Actually tests {@link IndexingItemHandlerInternal}.
	 */
	@Test
	public void testUnsupportedProgress() {
		ItemContentBufferStrategy strategy = mock(ItemContentBufferStrategy.class);
		IndexingItemHandler enable = new IndexingItemHandlerContentEnable(strategy);
		IndexingItemHandler disable = new IndexingItemHandlerContentDisable();
		
		IndexingItemProgress progressUnsupported = mock(IndexingItemProgress.class);
		try {
			enable.handle(progressUnsupported);
			fail("Expected exception on unsupported progress type");
		} catch (IllegalStateException e) {
			assertTrue("Got " + e, e.getMessage().startsWith("Configuration error"));
		}
		try {
			disable.handle(progressUnsupported);
			fail("Expected exception on unsupported progress type");
		} catch (IllegalStateException e) {
			assertTrue("Got " + e, e.getMessage().startsWith("Configuration error"));
		}
	}	
	
}
