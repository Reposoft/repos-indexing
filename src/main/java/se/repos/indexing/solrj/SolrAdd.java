package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

public class SolrAdd extends SolrOp {

	private SolrInputDocument doc;

	public SolrAdd(SolrServer core, SolrInputDocument doc) {
		super(core);
		this.doc = doc;
	}

	@Override
	public void runOp() throws SolrServerException, IOException {
		core.add(doc);
	}

}
