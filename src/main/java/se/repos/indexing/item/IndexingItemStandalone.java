/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.io.InputStream;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Used to run extraction on a standalone file from classpath, without actually adding to indexing, used for testing.
 */
public class IndexingItemStandalone implements IndexingItemProgress {

	private ClassLoader loader;
	private String resource;
	private IndexingDoc fields;
	private CmsChangesetItem item;
	private CmsRepository repository = null;
	private RepoRevision revision = null;

	public IndexingItemStandalone(CmsRepository repository, RepoRevision indexingRevision, CmsChangesetItem item) {
		this.repository = repository;
		this.revision = indexingRevision;
		this.fields = new Fields();
		this.item = item;
	}
	
	public IndexingItemStandalone(String classLoaderResource) {
		this.loader = getClass().getClassLoader();
		this.resource = classLoaderResource;
		this.fields = new Fields();
		this.item = new ContentsOnlyItem();
	}

	@Override
	public IndexingDoc getFields() {
		return fields;
	}
	
	@Override
	public InputStream getContents() {
		if (resource == null) {
			throw new IllegalStateException("No resource provided for this item");
		}
		InputStream resourceAsStream = loader.getResourceAsStream(resource);
		if (resourceAsStream == null) {
			throw new IllegalArgumentException("Failed to load classpath resource: " + resource);
		}
		return resourceAsStream;
	}
	
	@Override
	public CmsItemProperties getProperties() {
		throw new UnsupportedOperationException("Not supported in standalone extraction, yet");
	}
	
	@Override
	public CmsRepository getRepository() {
		if (repository == null) {
			throw new IllegalStateException("No repository provided for this item");
		}
		return repository;
	}

	@Override
	public RepoRevision getRevision() {
		if (revision == null) {
			throw new IllegalStateException("No revision provided for this indexing run");
		}
		return revision;
	}

	@Override
	public CmsChangesetItem getItem() {
		return item;
	}
	
	@SuppressWarnings("serial")
	class Fields extends IndexingDocIncrementalSolrj {

		@Override
		public IndexingDoc deepCopy() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}
		
	}
	
	private class ContentsOnlyItem implements CmsChangesetItem {

		@Override
		public boolean isFile() {
			return true;
		}

		@Override
		public boolean isFolder() {
			return false;
		}
		
		@Override
		public boolean isDelete() {
			return false;
		}

		@Override
		public boolean isCopy() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isAdd() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isReplace() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isMove() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isContentModified() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isContent() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isPropertiesModified() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isProperties() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isExplicit() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isDerived() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public boolean isOverwritten() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public CmsItemPath getPath() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public RepoRevision getRevision() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public CmsItemPath getCopyFromPath() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public RepoRevision getCopyFromRevision() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public CmsChangesetItem getPreviousChange() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}

		@Override
		public RepoRevision getRevisionObsoleted() {
			throw new UnsupportedOperationException("Not supported in standalone extraction");
		}
		
	}
	
}
