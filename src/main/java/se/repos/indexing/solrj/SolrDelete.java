package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

public class SolrDelete extends SolrOp {

	public static final long COMMIT_WITHIN_MILLIS = 0;
	
	private String query;
	
	public SolrDelete(SolrServer core, String query) {
		super(core);
		this.query = query;
	}

	@Override
	public void runOp() throws SolrServerException, IOException {
		core.deleteByQuery(query, 0);
	}

}
