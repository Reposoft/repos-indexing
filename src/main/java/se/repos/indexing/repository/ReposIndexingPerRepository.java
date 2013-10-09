/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexingEventAware;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBufferDeleted;
import se.repos.indexing.item.ItemContentBufferFolder;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.item.ItemPropertiesDeleted;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.repos.indexing.scheduling.IndexingUnitRevision;
import se.repos.indexing.scheduling.Marker;
import se.repos.indexing.solrj.MarkerCommitSolrjRepositem;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.repos.indexing.twophases.IndexingItemProgressPhases;
import se.repos.indexing.twophases.RepositoryIndexStatus;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public class ReposIndexingPerRepository implements ReposIndexing {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private CmsRepository repository;
	private IndexingSchedule schedule;
	private CmsChangesetReader changesetReader;
	private CmsRepositoryLookup revisionLookup;
	private Set<IndexingItemHandler> handlers;

	private RepoRevision scheduledHighest = null;
	private RepoRevision scheduledLowest = null;
	private RepoRevision completedHighest = null;

	private RepositoryIndexStatus repositoryStatus = null;
	
	@Inject
	public ReposIndexingPerRepository(CmsRepository repository) {
		this.repository = repository;
	}
	
	@Inject
	public void setRepositoryStatus(RepositoryIndexStatus repositoryStatus) {
		this.repositoryStatus  = repositoryStatus;
	}
	
	@Inject
	public void setIndexingSchedule(IndexingSchedule schedule) {
		this.schedule = schedule;
	}
	
//	@Inject
//	public void setSolrRepositem(@Named("repositem") SolrServer repositem) {
//		this.repositem = repositem;
//	}
//	
	@Inject
	public void setCmsChangesetReader(CmsChangesetReader changesetReader) {
		this.changesetReader = changesetReader;
	}
	
	@Inject
	public void setHandlers(Set<IndexingItemHandler> handlers) {
		this.handlers = handlers;
		for (IndexingItemHandler h : handlers) {
			if (h instanceof IndexingEventAware) {
				throw new IllegalArgumentException("Handler " + h + " not accepted because this indexing impl does not support " + IndexingEventAware.class.getSimpleName());
			}
		}
	}

	@Inject
	public void setRevisionLookup(@Named("inspection") CmsRepositoryLookup lookup) {
		this.revisionLookup = lookup;
	}
	
//
//	/**
//	 * Optional, adds event listeners that are not added through as other types of dependencies.
//	 * 
//	 * {@link IndexingEventAware} is detected from the following:
//	 * <ul>
//	 * <li>{@link #setItemBlocking(Set)}</li>
//	 * <li>{@link #setItemBackground(Set)}</li>
//	 * <li>{@link #setItemContentsBufferStrategy(ItemContentsBufferStrategy)}</li>
//	 * <li>{@link #setItemPropertiesBufferStrategy(ItemPropertiesBufferStrategy)}</li>
//	 */
//	@Inject
//	public void addEventListeners(Set<IndexingEventAware> other) {
//		if (other != null) {
//			this.eventHandlers.addAll(other);
//		}
//	}
//	
//	protected Collection<IndexingEventAware> getListerners() {
//		return eventHandlers;
//	}
//	

	@Override
	public void sync(CmsRepository repositoryParamDeprecated, RepoRevision revision) {
		throw new IllegalArgumentException("Repository parameter is deprecated");
	}
	
	@Override
	public void sync(RepoRevision revision) {
		
		logger.info("Sync requested {} rev {}", repository, revision);
		if (revision.getDate() == null) {
			throw new IllegalArgumentException("Revision must be qualified with timestamp, got " + revision);
		}
		
		/*
		At large sync operations, do we run all blocking indexing first and then all background, or do we need more sophistication?
		Do we completely rule out other ongoing tasks than those executed by this instance?
		How do we handle indexing errors so we don't index that revision again and again?
		
		 */
		
		if (scheduledHighest == null) {
			logger.info("Unknown index completion status for repository {}. Polling.", repository);
			completedHighest = repositoryStatus.getIndexedRevisionHighestCompleted(repository);
			scheduledLowest = repositoryStatus.getIndexedRevisionLowestStarted(repository);
			scheduledHighest = repositoryStatus.getIndexedRevisionHighestStarted(repository);
			if (scheduledLowest == null) {
				if (scheduledHighest != null) {
					logger.error("Inconsistent revision query results, got highest in progress {}", scheduledHighest);
				}
				logger.info("Indexing has completed revision {}, no indexing in progress", completedHighest);
			} else {
				logger.info("Indexing has completed revision {}, in progress from {} to {}", completedHighest, scheduledLowest, scheduledHighest);
			}
		}
		
		// running may be null if everything is completed
		if (scheduledHighest == null) {
			logger.debug("No revision status in index. Starting from 0.");
			scheduledHighest = new RepoRevision(0, revisionLookup.getRevisionTimestamp(repository, 0));
		}
		
		for (long i = scheduledHighest.getNumber() + 1; i <= revision.getNumber(); i++) {
			Date revt = revisionLookup.getRevisionTimestamp(repository, i);
			logger.debug("Creating indexing unit {} {}", i, revt);
			RepoRevision next = new RepoRevision(i, revt);
			IndexingUnitRevision read = getIndexingUnit(next, revision);
			schedule.add(read);
		}

	}

	/**
	 * @param revision The revision to index
	 * @param referenceRevision Reference revision, for checking head status
	 */
	protected IndexingUnitRevision getIndexingUnit(RepoRevision revision, RepoRevision referenceRevision) {
		logger.debug("Reading changeset {}{}", revision, referenceRevision == null ? "" : " with reference revision " + referenceRevision);
		CmsChangeset changeset = changesetReader.read(revision, referenceRevision);

		CmsItemProperties revprops = null;
		String commitId = repositoryStatus.indexRevStart(repository, revision, revprops);
		
		List<IndexingItemProgress> items = new LinkedList<IndexingItemProgress>();
		for (CmsChangesetItem item : changeset.getItems()) {
			
			IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
			doc.addField("revid", commitId);
			
			IndexingItemProgressPhases progress = new IndexingItemProgressPhases(repository, revision, item, doc);
			
			items.add(progress);
		}

		return new IndexingUnitRevision(items, handlers);	
	}

	@Override
	public RepoRevision getRevComplete(CmsRepository repository) {
		if (repository != null) {
			throw new IllegalArgumentException("Repository parameter is deprecated");
		}
		return completedHighest;
	}

	@Override
	public RepoRevision getRevProgress(CmsRepository repository) {
		if (repository != null) {
			throw new IllegalArgumentException("Repository parameter is deprecated");
		}
		return scheduledLowest;
	}
	
}
