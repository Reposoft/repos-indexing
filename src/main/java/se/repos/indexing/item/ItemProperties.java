/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.Set;

import se.repos.indexing.CoreCommit;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Versioned properties of an item,
 * assumed to run after {@link ItemPathinfo}.
 */
public class ItemProperties implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		// TODO Auto-generated method stub
	}

	@Override
	public CoreCommit getCommit() {
		return null;
	}
	
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onRevisionBegin(CmsRepository repository, RepoRevision revision) {
	}

	@Override
	public void onRevisionEnd(CmsRepository repository, RepoRevision revision) {
	}	

}
