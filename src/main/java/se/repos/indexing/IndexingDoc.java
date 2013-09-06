/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import java.util.Collection;

public interface IndexingDoc {

	/**
	 * Supports incremental adding to multivalue fields.
	 * @param name
	 * @param value
	 */
	public void addField(String name, Object value);
	
	public void setField(String name, Object value);
	
	public void removeField(String name);	
	
	public Object getFieldValue(String name);

	public Collection<Object> getFieldValues(String name);
	
	public boolean containsKey(String fieldName);
	
	public Collection<String> getFieldNames();
	
	/**
	 * @return the number of fields
	 */
	public int size();
	
	/**
	 * Useful for reusing fields in a different core.
	 * @return
	 */
	public IndexingDoc deepCopy();
	
}
