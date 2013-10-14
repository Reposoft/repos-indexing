/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import se.repos.indexing.IdStrategy;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

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
	
	@Override
	public String getIdRepository(CmsRepository repository) {
		return repository.getHost() + repository.getUrlAtHost();
	}
	
	@Override
	public String getId(CmsRepository repository, RepoRevision revision, CmsItemPath path) {
		return getIdHead(repository, path) + getPegSeparator() + getIdRevision(revision); 
	}

	@Override
	public String getIdHead(CmsRepository repository, CmsItemPath path) {
		return getIdRepository(repository) + (path == null ? getRootPath() : path); 
	}
	
	@Override
	public String getId(CmsItemId itemId) {
		return getId(itemId.getRepository(), getRevision(itemId), itemId.getRelPath());
	}
	
	@Override
	public String getIdHead(CmsItemId itemId) {
		return getIdHead(itemId.getRepository(), itemId.getRelPath());
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
