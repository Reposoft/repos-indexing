package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.Marker;

public class MarkerCommitSolrj implements Marker {

	private SolrServer core;

	public MarkerCommitSolrj(SolrServer core) {
		this.core = core;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void onItemsMark() {
		new SolrCommit(core).run();
	}

}
