/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.HandlerIteration.MarkerDecision;
import se.repos.indexing.solrj.HandlerSendIncrementalSolrjRepositem;

public class HandlerIterationTest {

	@Test
	public void testNoInterrupt() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class);
		IndexingItemProgress item2 = mock(IndexingItemProgress.class);
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		List<HandlerCall> calls = new LinkedList<HandlerCall>();
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		handlers.add(new Handler1(calls));
		handlers.add(new Handler2(calls));
		handlers.add(new Marker1(calls));
		handlers.add(new Handler3(calls));
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
		
		HandlerIteration iteration = new HandlerIteration(new MarkerDecision() {
			@Override
			public boolean before(IndexingItemHandler handler, IndexingItemProgress item) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean before(Marker marker) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean after(Marker marker) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		
		iteration.proceed(unit);
		
	}
	
	private class HandlerCall {
		
		IndexingItemHandler handler;
		IndexingItemProgress item;
		boolean isTrigger;
		boolean isIgnore;
		
		HandlerCall(IndexingItemHandler handler, IndexingItemProgress progress) {
			this.handler = handler;
			this.item = progress;
			this.isTrigger = false;
			this.isIgnore = false;			
		}
		
		HandlerCall(Marker marker, boolean isTrigger) {
			this.handler = marker;
			this.item = null;
			this.isTrigger = isTrigger;
			this.isIgnore = !isTrigger;
		}
		
	}
	
	private class TraceHandler implements IndexingItemHandler {

		protected Collection<HandlerCall> calls;

		TraceHandler(Collection<HandlerCall> calls) {
			this.calls = calls;
		}
		
		@Override
		public void handle(IndexingItemProgress progress) {
			calls.add(new HandlerCall(this, progress));
		}

		@Override
		public Set<Class<? extends IndexingItemHandler>> getDependencies() {
			return null;
		}
		
	}
	
	private class TraceMarker extends TraceHandler implements Marker {

		TraceMarker(Collection<HandlerCall> calls) {
			super(calls);
		}
		
		@Override
		public void trigger() {
			calls.add(new HandlerCall(this, true));
		}

		@Override
		public void ignore() {
			calls.add(new HandlerCall(this, false));
		}
		
	}
	
	private class Handler1 extends TraceHandler {  Handler1(Collection<HandlerCall> calls) { super(calls); } }
	private class Handler2 extends TraceHandler {  Handler2(Collection<HandlerCall> calls) { super(calls); } }
	private class Handler3 extends TraceHandler {  Handler3(Collection<HandlerCall> calls) { super(calls); } }
	private class Handler4 extends TraceHandler {  Handler4(Collection<HandlerCall> calls) { super(calls); } }
	private class Marker1 extends TraceMarker {  Marker1(Collection<HandlerCall> calls) { super(calls); } }
	private class Marker2 extends TraceMarker {  Marker2(Collection<HandlerCall> calls) { super(calls); } }
	private class Marker3 extends TraceMarker {  Marker3(Collection<HandlerCall> calls) { super(calls); } }

}
