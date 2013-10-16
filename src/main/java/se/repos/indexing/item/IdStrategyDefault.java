/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import se.repos.indexing.IdStrategy;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;

public class IdStrategyDefault implements IdStrategy {

	/**
	 * @return defaults to empty string because paths have no trailing slash
	 */
	protected String getRootPath() {
		return "";
	}
	
	/**
	 * @return what's inbetween the idhead and the revision identifier
	 */
	protected String getPegSeparator() {
		return "@";
	}
	
	protected String getInfoSeparator() {
		return "#";
	}
	
	/**
	 * @param itemId
	 * @return full revision from itemId, which currently has only the number
	 */
	protected RepoRevision getRevision(CmsItemId itemId) {
		if (itemId.getPegRev() == null) {
			throw new IllegalArgumentException("Item must have revision to be valid as ID");
		}
		return new RepoRevision(itemId.getPegRev(), null);
	}
	
	/**
	 * @param revision
	 * @return how to represent a revision in id
	 */
	@Override
	public String getIdRevision(RepoRevision revision) {
		return Long.toString(revision.getNumber());
	}
	
	protected String getIdHost(CmsRepository repository) {
		return repository.getHost();
	}
	
	@Override
	public String getIdRepository(CmsRepository repository) {
		return getIdHost(repository) + repository.getUrlAtHost();
	}
	
	@Override
	public String getId(CmsItemId itemId, RepoRevision revision) {
		if (revision == null) {
			throw new IllegalArgumentException("Use getIdHead for id without revision");
		}
		if (itemId.getPegRev() != null && !revision.equals(new RepoRevision(itemId.getPegRev(), null))) {
			throw new IllegalArgumentException("Provided revision " + revision + " does not match id "+ itemId);
		}
		return getIdHead(itemId) + getPegSeparator() + getIdRevision(revision); 
	}
	
	@Override
	public String getIdHead(CmsItemId itemId) {
		return itemId.getRepository().getHost() + itemId.getUrlAtHost();
	}
	
	@Override
	public String getId(CmsRepository repository, RepoRevision revision, CmsItemPath path) {
		return getId(new CmsItemIdUrl(repository, path), revision); 
	}

	@Override
	public String getIdHead(CmsRepository repository, CmsItemPath path) {
		return getIdHead(new CmsItemIdUrl(repository, path));
	}
	
	@Override
	public String getIdCommit(CmsRepository repository, RepoRevision revision) {
		return getIdEntry(repository, getIdRevision(revision));
	}

	@Override
	public String getIdEntry(CmsRepository repository, String repositoryField) {
		return getIdRepository(repository) + getInfoSeparator() + repositoryField;
	}	
	
}
