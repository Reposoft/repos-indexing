/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public interface ItemPropertiesBufferStrategy {

	/**
	 * Called for files and folders, returns per pegged item access to versioned metadata.
	 * @param repository
	 * @param revision
	 * @param path
	 * @return the property data, or proxy for delayed reading of it
	 */
	CmsItemProperties getProperties(CmsRepositoryInspection repository, RepoRevision revision, CmsItemPath path);
	
}
