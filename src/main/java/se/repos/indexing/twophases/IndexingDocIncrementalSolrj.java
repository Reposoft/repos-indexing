/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingDocIncremental;

public class IndexingDocIncrementalSolrj implements
		IndexingDocIncremental, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	static final SolrInputDocument UPDATE_MODE_NO_CHANGES = new SolrInputDocument();
	
	private boolean update = false;
	private Set<String> fieldsUpdated = new HashSet<String>();

	private SolrInputDocument doc;
	
	private int contentSize = 0;
	
	public IndexingDocIncrementalSolrj() {
		this(new SolrInputDocument(), 0);
	}
	
	protected IndexingDocIncrementalSolrj(SolrInputDocument doc, int contentSize) {
		this.doc = doc;
		this.contentSize = contentSize;
	}
	
	/**
	 * Used to do index updates after the meta indexing round,
	 * in handlers that need existing field values.
	 * Sets update mode to true because the item obviously exists already.
	 * @param queryResult Stored fields from the index
	 */
	protected IndexingDocIncrementalSolrj(SolrDocument queryResult) {
		this.update = true;
		// probably needed when reindexing runs all revisions blocking first, then starts backrground for all revisions
		throw new UnsupportedOperationException("not implemented");
	}
	
	public SolrInputDocument getSolrDoc() {
		if (!update) {
			return doc;
		}
		if (fieldsUpdated.size() == 0) {
			return UPDATE_MODE_NO_CHANGES;
		}
		SolrInputDocument d = doc.deepCopy();
		for (String f : new ArrayList<String>(d.keySet())) {
			if ("id".equals(f) || fieldsUpdated.contains(f)) {
				
			} else {
				d.remove(f);
			}
		}
		return d;
	}
	
	@Override
	public void setUpdateMode(boolean fieldSetIsPartialUpdate) {
		if (!fieldSetIsPartialUpdate) {
			throw new UnsupportedOperationException("support for disabling update mode is not implemented");
		}
		update = fieldSetIsPartialUpdate;
	}

	private void countSize(Object value, int multiplier) {
		if (value instanceof String) {
			int s = ((String) value).length();
			this.contentSize += s * multiplier;
		}
	}
	
	@Override
	public void setField(String name, Object value) {
		if (update) {
			value = new PartialUpdateVal(value);
			fieldsUpdated.add(name);
		}
		countSize(value, 1);
		doc.setField(name, value);
	}

	@Override
	public void addField(String name, Object value) {
		if (update) {
			if (!fieldsUpdated.contains(name)) {
				doc.remove(name);
			}
			value = new PartialUpdateAdd(value);
			fieldsUpdated.add(name);
		}
		countSize(value, 1);
		doc.addField(name, value);
	}	

	@Override
	public void removeField(String fieldName) {
		if (update) {
			throw new UnsupportedOperationException("Remove not supported in update mode yet");
		}
		countSize(doc.getFieldValue(fieldName), -1);
		doc.removeField(fieldName);
	}
	
	@Override
	public Object getFieldValue(String name) {
		if (fieldsUpdated.contains(name)) {
			Object v = doc.getFieldValue(name);
			return ((Map<?, ?>) v).values().iterator().next();
		}
		return doc.getFieldValue(name);
	}

	@Override
	public Collection<Object> getFieldValues(String name) {
		if (fieldsUpdated.contains(name)) {
			throw new UnsupportedOperationException("Update field get not implemented for multi-value");
		}
		return doc.getFieldValues(name);
	}

	@Override
	public Collection<String> getFieldNames() {
		return doc.getFieldNames();
	}
	
	@Override
	public boolean containsKey(String fieldName) {
		return doc.containsKey(fieldName);
	}
	
	@Override
	public int size() {
		if (update) {
			return fieldsUpdated.size();
		}
		return doc.size();
	}
	
	@Override
	public IndexingDoc deepCopy() {
		if (fieldsUpdated.size() > 0) {
			throw new UnsupportedOperationException("Field clone not supported when there are field updates to " + fieldsUpdated);
		}
		return new IndexingDocIncrementalSolrj(doc.deepCopy(), contentSize);
	}

	private class PartialUpdateVal extends HashMap<String, Object> {
		private static final long serialVersionUID = 1L;
		private PartialUpdateVal(Object fieldValue) {
			super(1);
			put("set", fieldValue);
		}
	}
	
	private class PartialUpdateAdd extends HashMap<String, Object> {
		private static final long serialVersionUID = 1L;
		private PartialUpdateAdd(Object fieldValue) {
			super(1);
			put("add", fieldValue);
		}
	}

	@Override
	public int getContentSize() {
		return contentSize;
	}

}
