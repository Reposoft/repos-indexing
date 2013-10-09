package se.repos.indexing;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class IndexingHandlersTest {

	@Test
	public void testTo() {
		final IndexingItemHandler handler1 = new Handler("1");
		final IndexingItemHandler handler2 = new Handler("2");
		Module module = new AbstractModule() {
			@Override
			protected void configure() {
				Multibinder<IndexingItemHandler> multibinder = Multibinder.newSetBinder(binder(), IndexingItemHandler.class);
				IndexingHandlers.to(multibinder, HandlerPathinfo.class, HandlerProperties.class);
				IndexingHandlers.toInstance(multibinder, handler1, handler2);
			}
		};
		Injector injector = Guice.createInjector(module);
		Map<Key<?>, Binding<?>> allBindings = injector.getAllBindings();
		assertTrue(allBindings.keySet().contains(Key.get(new TypeLiteral<Set<IndexingItemHandler>>(){})));
		assertTrue(allBindings.keySet().contains(Key.get(HandlerPathinfo.class)));
		assertTrue(allBindings.keySet().contains(Key.get(HandlerProperties.class)));
		Set<IndexingItemHandler> handlers = injector.getInstance(Key.get(new TypeLiteral<Set<IndexingItemHandler>>(){}));
		assertTrue(handlers.contains(handler1));
		assertTrue(handlers.contains(handler2));
	}
	
	private static class Handler implements IndexingItemHandler {
		
		Handler(String string) {
		}

		@Override
		public void handle(IndexingItemProgress progress) {
		}

		@Override
		public Set<Class<? extends IndexingItemHandler>> getDependencies() {
			return null;
		}
		
	}

}
