/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

public interface ReposIndexing {

	@Deprecated // service should be per repository
	public void sync(CmsRepository repository, RepoRevision revision);

	public void sync(RepoRevision revision);
	
	/**
	 * @return Highest revision that indexing has completed for.
	 */
	public RepoRevision getRevComplete(CmsRepository repository);

	/**
	 * @return Highest revision that indexing has started for.
	 */
	public RepoRevision getRevProgress(CmsRepository repo);
	
}
