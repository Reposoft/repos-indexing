package se.repos.indexing.solrj;

import java.util.Set;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.Marker;

public class CommitSolrj implements Marker {

	public CommitSolrj() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onItemsMark() {
		// TODO Auto-generated method stub

	}

}
