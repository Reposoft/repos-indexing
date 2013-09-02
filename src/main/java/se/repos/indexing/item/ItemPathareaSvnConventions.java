/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.HashSet;
import java.util.Set;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Sets value in the "patharea" field,
 * recognizing the conventional svn trunk/branches/tags.
 * The field is multi-value so additional interpretators of path conventions can be registered and add values.
 */
public class ItemPathareaSvnConventions implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		throw new UnsupportedOperationException("not implemented");
	}

	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			add(ItemPathinfo.class);
		}};
	}
	
	@Override
	public void onRevisionBegin(CmsRepository repository, RepoRevision revision) {
	}

	@Override
	public void onRevisionEnd(CmsRepository repository, RepoRevision revision) {
	}	

}
