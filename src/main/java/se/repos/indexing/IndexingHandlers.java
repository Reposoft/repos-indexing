package se.repos.indexing;

import java.util.LinkedList;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.ItemPathinfo;
import se.repos.indexing.item.ItemProperties;
import se.repos.indexing.repository.IndexingItemHandlerContentDisable;
import se.repos.indexing.repository.IndexingItemHandlerContentEnable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesDisable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesEnable;
import se.repos.indexing.scheduling.IndexingScheduleBlockingOnly;
import se.repos.indexing.scheduling.ScheduleAwaitNewer;
import se.repos.indexing.scheduling.ScheduleSendIncremental;
import se.repos.indexing.scheduling.ScheduleBackground;
import se.repos.indexing.scheduling.ScheduleSendComplete;

public abstract class IndexingHandlers {

	public static final Iterable<Class<? extends IndexingItemHandler>> STANDARD_FIRST = new LinkedList<Class<? extends IndexingItemHandler>>() {
		private static final long serialVersionUID = 1L;
		{
			add(ScheduleBackground.class);
			add(ItemPathinfo.class);
			add(IndexingItemHandlerPropertiesEnable.class);
			add(ItemProperties.class);
			add(IndexingItemHandlerPropertiesDisable.class);
			add(ScheduleSendIncremental.class);
			add(ScheduleAwaitNewer.class);
			add(IndexingItemHandlerContentEnable.class);
		}
	};

	public static final Iterable<Class<? extends IndexingItemHandler>> STANDARD_LAST = new LinkedList<Class<? extends IndexingItemHandler>>() {
		private static final long serialVersionUID = 1L;
		{
			add(IndexingItemHandlerContentDisable.class);
			add(ScheduleSendComplete.class);
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
		throw new UnsupportedOperationException("not implemented");
	}

	public static void to(Object guiceMultibinder, Class<? extends IndexingItemHandler>... handlers) {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public static void toInstance(Object guiceMultibinder, Iterable<? extends IndexingItemHandler> handlers) {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public static void toInstance(Object guiceMultibinder, IndexingItemHandler... handlers) {
		throw new UnsupportedOperationException("not implemented");
	}
	
}
