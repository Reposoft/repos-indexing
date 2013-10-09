/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

public class HandlerSendIncrementalSolrjRepositem extends
		HandlerSendIncrementalSolrj {

	@Inject
	public HandlerSendIncrementalSolrjRepositem(@Named("repositem") SolrServer solrCore) {
		super(solrCore);
	}

}
