/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

public interface ItemContentBufferStrategy {

	/**
	 * Called only for files, returns per pegged item access to content.
	 * @param repository TODO remove this argument or change to CmsRepository
	 * @param revision
	 * @param path
	 * @param pathinfo
	 * @return
	 */
	ItemContentBuffer getBuffer(CmsRepositoryInspection repository, RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo);
	
}
