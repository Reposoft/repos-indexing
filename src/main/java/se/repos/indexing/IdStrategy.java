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
	 * @return same as {@link #getId(CmsRepository, RepoRevision, CmsItemPath)}
	 */
	public String getId(CmsItemId itemId);
	
	/**
	 * @return same as {@link #getIdHead(CmsRepository, CmsItemPath)}
	 */
	public String getIdHead(CmsItemId itemId);

	/**
	 * @return the prefix to commit items, i.e. repository with a separator that is different from {@link #getId(CmsRepository, RepoRevision, CmsItemPath)} with root path
	 */
	public String getIdRepository(CmsRepository repository);
	
	/**
	 * @return id for item that represents commit, i.e. indexing status and revprops
	 */
	public String getIdCommit(CmsRepository repository, RepoRevision revision);

	/**
	 * @return the format of a revision
	 */
	public String getIdRevision(RepoRevision revision);
	
}
