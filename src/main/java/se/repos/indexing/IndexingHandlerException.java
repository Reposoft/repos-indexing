/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

/**
 *
 * @author Markus Mattsson
 */
public class IndexingHandlerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IndexingHandlerException(String message) {
		this(message, null);
	}
	
	public IndexingHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
