/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;

import javax.inject.Named;

public class MarkerCommitSolrjRepositem extends MarkerCommitSolrj {

	@Inject
	public MarkerCommitSolrjRepositem(@Named("repositem") SolrClient repositem) {
		super(repositem);
	}

}
