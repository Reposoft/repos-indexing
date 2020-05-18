/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;

public class HandlerSendIncrementalSolrjRepositem extends
		HandlerSendIncrementalSolrj {

	@Inject
	public HandlerSendIncrementalSolrjRepositem(@Named("repositem") SolrClient solrCore) {
		super(solrCore);
	}

}
