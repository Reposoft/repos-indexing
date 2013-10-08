package se.repos.indexing.solrj;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;

import com.google.inject.name.Named;

public class MarkerCommitSolrjRepositem extends MarkerCommitSolrj {

	@Inject
	public MarkerCommitSolrjRepositem(@Named("repositem") SolrServer repositem) {
		super(repositem);
	}

}
