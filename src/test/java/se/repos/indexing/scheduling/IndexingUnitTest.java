package se.repos.indexing.scheduling;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class IndexingUnitTest {

	@Test
	public void test() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class);
		IndexingItemProgress item2 = mock(IndexingItemProgress.class);
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		IndexingItemHandler handler1 = mock(IndexingItemHandler.class);
		IndexingItemHandler handler2 = mock(IndexingItemHandler.class);
		
		Set<IndexingItemHandler> handlers = new LinkedHashSet<IndexingItemHandler>();
		handlers.add(handler1);
		handlers.add(handler2);
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
	}

	@Test
	public void testMarkerReappear() {
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		
	}
	
}
