package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

public class SolrCommit extends SolrOp {

	public SolrCommit(SolrServer core) {
		super(core);
	}

	@Override
	public void runOp() throws SolrServerException, IOException {
		core.commit();
	}

}
