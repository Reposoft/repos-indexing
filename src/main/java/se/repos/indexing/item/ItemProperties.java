/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.Set;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Versioned properties of an item, all verbatim values from svn into the "prop_" dynamic field.
 */
public class ItemProperties implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc doc = progress.getFields();
		CmsItemProperties properties = progress.getProperties();
		for (String n : properties.getKeySet()) {
			doc.setField(getFieldName(n), properties.getString(n));
		}
	}
	
	protected String getFieldName(String propertyName) {
		return "prop_" + propertyName.replace(':', '.');
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}
	
}
