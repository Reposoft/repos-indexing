package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.ScheduleSendIncremental;

public class ScheduleSendIncrementalSolrj implements ScheduleSendIncremental {

	private SolrServer solr;

	public ScheduleSendIncrementalSolrj(SolrServer solrCore) {
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
