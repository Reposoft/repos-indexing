package se.repos.indexing.solrj;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;

import com.google.inject.name.Named;

public class HandlerSendSolrjRepositem extends HandlerSendSolrj {

	@Inject
	public HandlerSendSolrjRepositem(@Named("repositem") SolrServer core) {
		super(core);
	}

}
