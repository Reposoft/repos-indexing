package se.repos.indexing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.ItemPathinfo;
import se.repos.indexing.item.ItemProperties;
import se.repos.indexing.repository.IndexingItemHandlerContentDisable;
import se.repos.indexing.repository.IndexingItemHandlerContentEnable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesDisable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesEnable;
import se.repos.indexing.scheduling.ScheduleAwaitNewer;
import se.repos.indexing.scheduling.ScheduleBackground;
import se.repos.indexing.solrj.CommitSolrj;
import se.repos.indexing.solrj.ScheduleSendIncrementalSolrj;

public abstract class IndexingHandlers {

	public static final Iterable<Class<? extends IndexingItemHandler>> STANDARD_FIRST = new LinkedList<Class<? extends IndexingItemHandler>>() {
		private static final long serialVersionUID = 1L;
		{
			add(ScheduleBackground.class);
			add(ItemPathinfo.class);
			add(IndexingItemHandlerPropertiesEnable.class);
			add(ItemProperties.class);
			add(IndexingItemHandlerPropertiesDisable.class); // the others can read from indexing doc instead
			add(ScheduleSendIncrementalSolrj.class);
			add(ScheduleAwaitNewer.class);
			add(IndexingItemHandlerContentEnable.class);
		}
	};

	public static final Iterable<Class<? extends IndexingItemHandler>> STANDARD_LAST = new LinkedList<Class<? extends IndexingItemHandler>>() {
		private static final long serialVersionUID = 1L;
		{
			add(IndexingItemHandlerContentDisable.class);
			add(CommitSolrj.class);
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
