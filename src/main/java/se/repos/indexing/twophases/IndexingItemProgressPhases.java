/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.InputStream;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemProperties;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public class IndexingItemProgressPhases implements IndexingItemProgress {

	enum Phase {
		initial,
		update
	}
	
	private CmsRepository repository;
	private RepoRevision revision;
	private CmsChangesetItem item;
	private IndexingDocIncrementalSolrj fields;
	private CmsItemProperties properties;
	private ItemContentBuffer contents;

	public IndexingItemProgressPhases(CmsRepository repository, RepoRevision revision,
			CmsChangesetItem item, IndexingDocIncrementalSolrj fields) {
		this.repository = repository;
		this.revision = revision;
		this.item = item;
		this.fields = fields;
	}
	
	public void setPhase(Phase phase) {
		switch (phase) {
		case initial:
			throw new IllegalArgumentException("Can't switch to " + phase);
		case update:
			setPhaseUpdate();
		}
	}
	
	private void setPhaseUpdate() {
		fields.setUpdateMode(true);
	}

	/**
	 * @param itemVersionedMetadata possibly the actual properties, possibly a buffer i.e. deferred lookup
	 * @return for chaining
	 */
	public IndexingItemProgressPhases setProperties(CmsItemProperties itemVersionedMetadata) {
		this.properties = itemVersionedMetadata;
		return this;
	}
	
	public IndexingItemProgressPhases setContents(ItemContentBuffer buffer) {
		this.contents = buffer;
		return this;
	}
	
	@Override
	public CmsRepository getRepository() {
		return repository;
	}

	@Override
	public RepoRevision getRevision() {
		return revision;
	}

	@Override
	public IndexingDoc getFields() {
		return fields;
	}

	@Override
	public CmsChangesetItem getItem() {
		return item;
	}

	@Override
	public CmsItemProperties getProperties() {
		if (!hasPropertiesBuffer()) {
			throw new IllegalStateException("Indexing of properties is not enable at this stage");
		}
		return getPropertiesBuffer();
	}

	@Override
	public InputStream getContents() {
		if (!hasContentBuffer()) {
			throw new IllegalStateException("Indexing item contents not available at this stage");
		}
		return getContentBuffer().getContents();
	}

	public boolean hasContentBuffer() {
		return contents != null;
	}
	
	public ItemContentBuffer getContentBuffer() {
		return contents;
	}
	
	public boolean hasPropertiesBuffer() {
		return properties != null;
	}
	
	public CmsItemProperties getPropertiesBuffer() {
		return properties;
	}
	
}
