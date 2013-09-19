/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import se.simonsoft.cms.item.RepoRevision;

public interface IndexingEventAware {
	
	void onRevisionComplete(RepoRevision revision);
	
}
