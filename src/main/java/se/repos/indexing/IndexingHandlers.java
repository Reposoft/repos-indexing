/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;

import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.HandlerHeadinfo;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.repository.HandlerContentDisable;
import se.repos.indexing.repository.HandlerContentEnable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesDisable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesEnable;
import se.repos.indexing.repository.MarkerRevisionComplete;
import se.repos.indexing.scheduling.ScheduleAwaitNewer;
import se.repos.indexing.scheduling.ScheduleBackground;
import se.repos.indexing.solrj.HandlerSendIncrementalSolrjRepositem;
import se.repos.indexing.solrj.HandlerSendSolrjRepositem;
import se.repos.indexing.solrj.MarkerCommitSolrjRepositem;

public abstract class IndexingHandlers {

	public static final Iterable<Class<? extends IndexingItemHandler>> STANDARD_FIRST = new LinkedList<Class<? extends IndexingItemHandler>>() {
		private static final long serialVersionUID = 1L;
		{
			add(ScheduleBackground.class);
			add(HandlerHeadinfo.class);
			add(HandlerPathinfo.class);
			add(IndexingItemHandlerPropertiesEnable.class);
			add(HandlerProperties.class);
			add(IndexingItemHandlerPropertiesDisable.class); // the others can read from indexing doc instead
			add(HandlerSendIncrementalSolrjRepositem.class);
			add(ScheduleAwaitNewer.class);
			add(HandlerContentEnable.class);
			add(HandlerChecksum.class);
		}
	};

	public static final Iterable<Class<? extends IndexingItemHandler>> STANDARD_LAST = new LinkedList<Class<? extends IndexingItemHandler>>() {
		private static final long serialVersionUID = 1L;
		{
			add(HandlerContentDisable.class);
			add(HandlerSendSolrjRepositem.class);
			add(MarkerRevisionComplete.class);
			add(MarkerCommitSolrjRepositem.class);
		}
	};
	
	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	public static void configureFirst(Object guiceMultibinder) {
		to(guiceMultibinder, STANDARD_FIRST);
	}

	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	public static void configureLast(Object guiceMultibinder) {
		to(guiceMultibinder, STANDARD_LAST);
	}
	
	public static void to(Object guiceMultibinder, Iterable<Class<? extends IndexingItemHandler>> handlers) {
		avoidCompileTimeDependency(guiceMultibinder, "to", Class.class, handlers);
	}
	
	public static void to(Object guiceMultibinder, Class<? extends IndexingItemHandler>... handlers) {
		to(guiceMultibinder, (Iterable<Class<? extends IndexingItemHandler>>) Arrays.asList(handlers));
	}
	
	public static void toInstance(Object guiceMultibinder, Iterable<? extends IndexingItemHandler> handlers) {
		avoidCompileTimeDependency(guiceMultibinder, "toInstance", Object.class, handlers);
	}
	
	public static void toInstance(Object guiceMultibinder, IndexingItemHandler... handlers) {
		toInstance(guiceMultibinder, Arrays.asList(handlers));
	}
	
	/**
	 * Because we have such a useful utility here we're exposing it for wider use, no guarantees made.
	 */
	public static void toArbitrary(Object guiceBinder, @SuppressWarnings("rawtypes") Iterable to) {
		avoidCompileTimeDependency(guiceBinder, "to", Class.class, to);
	}
	
	private static void avoidCompileTimeDependency(Object guiceMultibinder, String bindToMethodName, Class<?> bindToType, @SuppressWarnings("rawtypes") Iterable bindings) {
		try {
			Method addBinding = guiceMultibinder.getClass().getDeclaredMethod("addBinding");
			addBinding.setAccessible(true);
			for (Object handler : bindings) {
				Object binder = addBinding.invoke(guiceMultibinder);
				Method to = binder.getClass().getMethod(bindToMethodName,bindToType);				
				to.invoke(binder, handler);
			}
		} catch (SecurityException e) {
			throw new RuntimeException("Error using multibinder at runtime", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Error using multibinder at runtime", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Error using multibinder at runtime", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error using multibinder at runtime", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error using multibinder at runtime", e);
		}		
	}
	
}
