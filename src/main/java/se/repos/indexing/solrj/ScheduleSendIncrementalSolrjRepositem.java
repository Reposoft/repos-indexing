package se.repos.indexing.solrj;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

public class ScheduleSendIncrementalSolrjRepositem extends
		ScheduleSendIncrementalSolrj {

	@Inject
	public ScheduleSendIncrementalSolrjRepositem(@Named("repositem") SolrServer solrCore) {
		super(solrCore);
	}

}
