/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

public class MarkerOptimizeSolrjRepositem extends MarkerOptimizeSolrj {

	public MarkerOptimizeSolrjRepositem(@Named("repositem") SolrServer core) {
		super(core);
	}

}
