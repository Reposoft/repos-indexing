/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentsBuffer;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * Needed because of how {@link ReposIndexingImpl} sets the buffer before any item handler is called (thus before item size is known)
 * and how this can support testing with sychronous run of fulltext etc.
 * 
 * Doesn't create the buffer until contents is actually asked for. Ideally this should happen only in the background phase.
 * 
 * Keeps a reference to the indexing doc for use when contents is actually requested.
 */
public class ItemContentsMemoryChoiceDeferred extends ItemContentsMemorySizeLimit {

	public final Logger logger = LoggerFactory.getLogger(ItemContentsMemoryChoiceDeferred.class);

	@Override
	public ItemContentsBuffer getBuffer(CmsRepositoryInspection repository,
			RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		return new BufferChoiceDeferred(repository, revision, path, pathinfo);
	}

	ItemContentsBuffer getBufferActual(CmsRepositoryInspection repository,
			RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		return super.getBuffer(repository, revision, path, pathinfo);
	}
	
	private class BufferChoiceDeferred implements ItemContentsBuffer {

		private CmsRepositoryInspection repository;
		private RepoRevision revision;
		private CmsItemPath path;
		private IndexingDoc indexingDoc;
		
		private ItemContentsBuffer bufferChoice = null;
		
		public BufferChoiceDeferred(CmsRepositoryInspection repository,
				RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
			this.repository = repository;
			this.revision = revision;
			this.path = path;
			this.indexingDoc = pathinfo;
		}

		@Override
		public InputStream getContents() {
			if (bufferChoice == null) {
				try {
					bufferChoice = getBufferActual(repository, revision, path, indexingDoc);
				} catch (RuntimeException e) {
					logger.debug("Deferred creation of contents buffer failed for {}@{}", path, revision);
					throw e;
				}
				logger.debug("Deferred creation of contents buffer invoked now for {}@{}, got {}", path, revision, bufferChoice.getClass());
			}
			return bufferChoice.getContents();
		}
		
	}
	
}
