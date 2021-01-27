/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.HandlerHeadClone;
import se.repos.indexing.item.HandlerIndexTime;
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

	/**
	 * This is not implemented, but we do need an API for extending the default chain.
	 * 
	 * There is {@link IndexingHandlers#configureFirst(Object)} and {@link IndexingHandlers#configureLast(Object)}
	 * but those are probably only usefulr for tests where you add a hanldler or two.
	 * 
	 * For custom ordering there's {@link Group} and {@link IndexingHandlers#STANDARD}, but those are difficult to use.
	 * 
	 * The actual need for extending is to insert handlers at various points,
	 * and possibly rearrange in cases like moving Background marker a few steps down.
	 */
	static interface HandlerChain {
		
	}	
	
	// with this we can change definition of the different handler groups and rename the steps
	public enum Group {
		Unblock,
		Structure,
		Fast,
		Nice,
		Content,
		Final
	}
	
	@SuppressWarnings("serial")
	public static final Map<Group, Iterable<Class<? extends IndexingItemHandler>>> STANDARD = Collections.unmodifiableMap(
		new HashMap<IndexingHandlers.Group, Iterable<Class<? extends IndexingItemHandler>>>() {{
			//put(Group.X, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
			//}}));
			put(Group.Unblock, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(ScheduleBackground.class);
			}}));
			put(Group.Structure, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerIndexTime.class);
				// No longer need to update head flag of previous revision.
				/*add(HandlerHeadinfo.class);*/
				add(HandlerPathinfo.class);
			}}));
			put(Group.Fast, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(IndexingItemHandlerPropertiesEnable.class);
				add(HandlerProperties.class);
				add(IndexingItemHandlerPropertiesDisable.class); // the others can read from indexing doc instead
			}}));
			put(Group.Nice, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerSendIncrementalSolrjRepositem.class);
				add(ScheduleAwaitNewer.class);
				add(HandlerContentEnable.class);
			}}));
			put(Group.Content, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerChecksum.class);
			}}));
			put(Group.Final, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerContentDisable.class);
				add(HandlerHeadClone.class); // Send head=true item, subsequent handlers processes the head=false item.
				add(HandlerSendSolrjRepositem.class);
				add(MarkerRevisionComplete.class);
				add(MarkerCommitSolrjRepositem.class);
				// do we need to optimize? we never delete from this core, except at clean/resync //add(MarkerOptimizeSolrjRepositem.class);
			}}));
		}});
	
	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	public static void configureFirst(Object guiceMultibinder) {
		to(guiceMultibinder, STANDARD.get(Group.Unblock));
		to(guiceMultibinder, STANDARD.get(Group.Structure));
		to(guiceMultibinder, STANDARD.get(Group.Fast));
		to(guiceMultibinder, STANDARD.get(Group.Nice));
		to(guiceMultibinder, STANDARD.get(Group.Content));
	}

	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	public static void configureLast(Object guiceMultibinder) {
		to(guiceMultibinder, STANDARD.get(Group.Final));
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
