/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class IndexingItemHandlerRunnable implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(IndexingItemHandlerRunnable.class);

	private IndexingItemHandler handler;
	private IndexingItemProgress progress;

	public IndexingItemHandlerRunnable(IndexingItemHandler handler,
			IndexingItemProgress progress) {
		this.handler = handler;
		this.progress = progress;
	}

	@Override
	public void run() {
		try {
			handler.handle(progress);
		} catch (IndexingHandlerException ex) {
			logger.warn(ex.getMessage());
			StringWriter stackTraceWriter = new StringWriter();
			ex.printStackTrace(new PrintWriter(stackTraceWriter));
			progress.getFields().addField("text_error", stackTraceWriter.toString());
		}
	}

}
