/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.Set;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Handles one item at a time
 */
public interface IndexingItemHandler {

	public void handle(IndexingItemProgress progress);
	
	/**
	 * Dependencies are used to verify order of execution among handlers,
	 * and also to provide only the expected field data from {@link IndexingDoc#deepCopy()}.
	 * @return other handlers that this one depends on, null for no dependencies
	 */
	public Set<Class<? extends IndexingItemHandler>> getDependencies();
	
	/**
	 * Called before this handler, together with others in the same phase, gets any items in a new revision.
	 * Invoked even if there are no (relevant) items in the revision (in case we implement listen filters).
	 * @param repository
	 * @param revision
	 */
	public void onRevisionBegin(CmsRepository repository, RepoRevision revision);
	
	/**
	 * Called after this handler, together with others in the same phase, will get no more items in the current revision.
	 * @param repository
	 * @param revision
	 */
	public void onRevisionEnd(CmsRepository repository, RepoRevision revision);
	
}
