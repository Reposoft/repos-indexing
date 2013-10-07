package se.repos.indexing.solrj;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.Marker;

public class RevisionBeginSolrj implements Marker {

	@Inject
	public RevisionBeginSolrj(@Named("repositem") SolrServer solrCore) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onItemsMark() {
		// TODO Auto-generated method stub

	}	
	
	@Override
	public void handle(IndexingItemProgress progress) {
		throw new IllegalStateException("This should be configure before any IndexingItemHandler so item processing should not have started");
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

}
