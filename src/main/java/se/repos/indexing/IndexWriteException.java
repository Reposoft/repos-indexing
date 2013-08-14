/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import org.apache.solr.client.solrj.SolrServerException;

public class IndexWriteException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IndexWriteException(SolrServerException e) {
		super(e);
	}

}
