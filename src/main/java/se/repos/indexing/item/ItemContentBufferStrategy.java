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
	 * @param repository TODO remove this argument and rely on per-repository services
	 * @param revision
	 * @param path
	 * @param pathinfo
	 * @return
	 */
	ItemContentBuffer getBuffer(CmsRepositoryInspection repository, RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo);
	
}
