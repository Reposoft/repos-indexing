package se.repos.indexing.repository;

import java.util.Collection;
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
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.repos.indexing.twophases.RepositoryStatus;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

public class ReposIndexingPerRepository implements ReposIndexing {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private CmsRepository repository;
	private IndexingSchedule schedule;
	private CmsChangesetReader changesetReader;
	private CmsRepositoryLookup revisionLookup;
	private Set<IndexingItemHandler> handlers;

	private RepoRevision scheduledHighest = null;

	private RepositoryStatus repositoryStatus = null;
	
	@Inject
	public ReposIndexingPerRepository(CmsRepository repository) {
		this.repository = repository;
	}
	
	@Inject
	public void setRepositoryStatus(RepositoryStatus repositoryStatus) {
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
		//eventHandlers.addIfAwareAll(handlersSync);
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
	public void sync(CmsRepository repository, RepoRevision revision) {
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
			RepoRevision c = repositoryStatus.getIndexedRevisionHighestCompleted(repository);
			RepoRevision pl = repositoryStatus.getIndexedRevisionLowestStarted(repository);
			RepoRevision ph = repositoryStatus.getIndexedRevisionHighestStarted(repository);
			if (pl == null) {
				if (ph != null) {
					logger.error("Inconsistent revision query results, got highest in progress {}", ph);
				}
				logger.info("Indexing has completed revision {}, no indexing in progress", c);
			} else {
				logger.info("Indexing has completed revision {}, in progress from {} to {}", c, pl, ph);
			}
			// for now the simplest solution is to assume that all in-progress operations have actually completed
			//if (pl != null) {
			//	logger.warn("Index contains unfinished revisions between {} and {} at first sync. Reindexing those.", pl, ph);
			//}
			scheduledHighest = ph;
		}
		
		// running may be null if everything is completed
		// TODO find the proper revision dates because those are indexed, needed in SvnTestIndexing too
		List<RepoRevision> range = new LinkedList<RepoRevision>();
		RepoRevision r = scheduledHighest;
		if (r == null) {
			logger.debug("No revision status in index. Starting from 0.");
			r = new RepoRevision(0, revisionLookup.getRevisionTimestamp(repository, 0));
		}
		for (long i = r.getNumber(); i <= revision.getNumber(); i++) {
			range.add(new RepoRevision(i, revisionLookup.getRevisionTimestamp(repository, i)));
		}
		logger.debug("Index range: {}", range);
		
		// run
		scheduledHighest = revision;
		if (repository instanceof CmsRepositoryInspection) {
			//sync((CmsRepositoryInspection) repository, changesetReader, revision, range);
		} else {
			throw new AssertionError("Admin repository instance required for indexing. Got " + repository.getClass());
		}

	}

	@Override
	public RepoRevision getRevComplete(CmsRepository repository) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RepoRevision getRevProgress(CmsRepository repo) {
		// TODO Auto-generated method stub
		return null;
	}

	class EventHandlers extends LinkedHashSet<IndexingEventAware> implements IndexingEventAware {

		private static final long serialVersionUID = 1L;

		protected void addIfAwareAll(Collection<? extends Object> possibleIndexingEventAware) {
			for (Object h : possibleIndexingEventAware) {
				addIfAware(h);
			}
		}	
		
		protected void addIfAware(Object possibleIndexingEventAware) {
			if (possibleIndexingEventAware instanceof IndexingEventAware) {
				this.add((IndexingEventAware) possibleIndexingEventAware);
			}
		}
		
		@Override
		public void onRevisionComplete(RepoRevision revision) {
			for (IndexingEventAware h : this) {
				h.onRevisionComplete(revision);
			}
		}
		
	}	
	
}
