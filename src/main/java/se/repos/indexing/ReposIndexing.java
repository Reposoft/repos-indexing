/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

public interface ReposIndexing {

	@Deprecated // service should be per repository
	public void sync(CmsRepository repository, RepoRevision revision);

	public void sync(RepoRevision revision);
	
	/**
	 * @return Highest revision that indexing has completed for.
	 * @deprecated Need to look at {@link IndexingSchedule}
	 */
	public RepoRevision getRevComplete(CmsRepository repository);

	/**
	 * @return Highest revision that indexing has started for.
	 * @deprecated Can not know if revisions have completed or not
	 */
	public RepoRevision getRevProgress(CmsRepository repo);

	/**
	 * Sort of an indexing lock, because further sync is only allowed above this revision.
	 * 
	 * We trust other instances to schedule that, but it must be the same {@link IndexingSchedule},
	 * or else the flagging of head/non-head would become incosistent
	 * where the updates to non-head happen before the actual indexing of that revision.
	 * 
	 * @return The revision up to which indexing is progressing or has completed.
	 */
	public RepoRevision getRevision();
	
}
