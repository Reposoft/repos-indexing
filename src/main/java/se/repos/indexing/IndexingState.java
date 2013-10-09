/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import se.repos.indexing.repository.MarkerRevisionComplete;
import se.repos.indexing.twophases.RepositoryIndexStatus;
import se.simonsoft.cms.item.RepoRevision;

/**
 * What's the type of reporting needed?
 *  - Service initialized
 *  - Sync requested (method?)
 *  - 
 *
 *
 *
 * It is useful to be able to reindex all the easily processed data quickly.
 * 
 * TODO in a long sync operation background queue becomes extremely long.
 * Should we instead query index for next rev to handle in background, and index phase?
 * 
 * TODO many methods here are specific to twophases, some listeners (for commit etc) just need their own phase
 * 
 * TODO combine with {@link RepositoryIndexStatus} and {@link MarkerRevisionComplete}
 */
public interface IndexingState {

	/**
	 * @return true if index is complete, disregarding a single in-progress revision that is ==HEAD
	 */
	boolean isIndexComplete();
	
	/**
	 * @return like {@link #isIndexComplete()} but requires only structure and item properties to be indexed
	 */
	boolean isIndexCompleteProps();
	
	void initializing(RepoRevision existingVerified, RepoRevision existingOverwriteFrom);

	/**
	 * 
	 * @param revision
	 */
	void syncRequest(RepoRevision revision);

	void syncComplete(RepoRevision revision);
	
	void blockingBegin(RepoRevision revision);
	
	void blockingComplete(RepoRevision revision);
	
	void backgroundBegin(RepoRevision revision);
	
	void backgroundPaused();
	
	void backgroundResumed();
	
	/**
	 * 
	 * @param revision
	 */
	void backgroundComplete(RepoRevision revision);

}
