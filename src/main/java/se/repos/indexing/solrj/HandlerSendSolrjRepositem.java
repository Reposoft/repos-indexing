/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;

import javax.inject.Named;

public class HandlerSendSolrjRepositem extends HandlerSendSolrj {

	@Inject
	public HandlerSendSolrjRepositem(@Named("repositem") SolrServer core) {
		super(core);
	}

}
