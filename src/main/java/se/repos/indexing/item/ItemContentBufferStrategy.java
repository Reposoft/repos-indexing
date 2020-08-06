/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;

public interface ItemContentBufferStrategy {

	/**
	 * Called only for files, returns per pegged item access to content.
	 * Refactored into a per-repo service.
	 * @param revision
	 * @param path
	 * @param pathinfo
	 * @return
	 */
	ItemContentBuffer getBuffer(RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo);
	
}
