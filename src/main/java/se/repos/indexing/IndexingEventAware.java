package se.repos.indexing;

import se.simonsoft.cms.item.RepoRevision;

public interface IndexingEventAware {
	
	void onRevisionComplete(RepoRevision revision);
	
}
