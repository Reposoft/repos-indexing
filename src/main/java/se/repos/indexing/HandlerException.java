/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

/**
 *
 * @author Markus Mattsson
 */
public class HandlerException extends RuntimeException {

	public HandlerException(String message) {
		this(message, null);
	}
	
	public HandlerException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
