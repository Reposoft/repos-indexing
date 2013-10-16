/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Produces different strings to identify repository contents such as files, folders, repositories and commits.
 */
public interface IdStrategy {

	/**
	 * @return full id for an index item such as a file or folder
	 */
	public String getId(CmsRepository repository, RepoRevision revision, CmsItemPath path);

	/**
	 * @return idhead field value
	 */
	public String getIdHead(CmsRepository repository, CmsItemPath path);
	
	/**
	 * @param revision needed because itemId does not contain a full revision
	 * @return same as {@link #getId(CmsRepository, RepoRevision, CmsItemPath)}
	 */
	public String getId(CmsItemId itemId, RepoRevision revision);
	
	/**
	 * @return same as {@link #getIdHead(CmsRepository, CmsItemPath)}
	 */
	public String getIdHead(CmsItemId itemId);

	/**
	 * @return prefix to both {@link #getIdCommit(CmsRepository, RepoRevision)} and {@link #getId(CmsItemId)}
	 */
	public String getIdRepository(CmsRepository repository);
	
	/**
	 * @return id for item that represents commit, i.e. indexing status and revprops
	 */
	public String getIdCommit(CmsRepository repository, RepoRevision revision);
	
	/**
	 * @return id for storing a piece of repository-related information
	 */
	public String getIdEntry(CmsRepository repository, String repositoryField);

	/**
	 * @return the format of a revision
	 */
	public String getIdRevision(RepoRevision revision);
	
}
