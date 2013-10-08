package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class HandlerSendIncrementalSolrj implements IndexingItemHandler {

	private SolrServer solr;

	public HandlerSendIncrementalSolrj(SolrServer solrCore) {
		this.solr = solrCore;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

}
