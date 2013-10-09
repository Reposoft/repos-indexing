/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingEventAware;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBufferDeleted;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemContentBufferFolder;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.item.ItemPropertiesDeleted;
import se.repos.indexing.repository.ReposIndexingPerRepository;
import se.repos.indexing.twophases.IndexingItemProgressPhases.Phase;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * 
 *	
 * TODO this service should be per-repository to allow different indexers
 * but for now it can be both, as the only extra complexity is handling in memory the latest revision indexed
 * TODO or should it be global so it can schedule indexing operations for all, with Map<CmsRepository,BackendService> dependencies?
 * 
 * @deprecated In favor of {@link ReposIndexingPerRepository}
 */
public class ReposIndexingImpl implements ReposIndexing {

	final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	SolrServer repositem;
	
	private CmsChangesetReader changesetReader;
	private CmsRepositoryLookup revisionLookup;
	
	private Map<CmsRepository, RepoRevision> scheduledHighest = new HashMap<CmsRepository, RepoRevision>();

	private Iterable<IndexingItemHandler> itemBlocking = new LinkedList<IndexingItemHandler>();
	private Iterable<IndexingItemHandler> itemBackground = new LinkedList<IndexingItemHandler>();

	private ItemContentBufferStrategy contentsBufferStrategy;
	private ItemPropertiesBufferStrategy propertiesBufferStrategy;
	
	private EventHandlers eventHandlers = new EventHandlers();
	
	private RepositoryIndexStatus repositoryStatus;
	
	@Inject
	public void setSolrRepositem(RepositoryIndexStatus repositoryStatus) {
		this.repositoryStatus = repositoryStatus;
	}
	
	@Inject
	public void setSolrRepositem(@Named("repositem") SolrServer repositem) {
		this.repositem = repositem;
	}
	
	@Inject
	public void setCmsChangesetReader(CmsChangesetReader changesetReader) {
		this.changesetReader = changesetReader;
	}
	
	@Inject
	public void setItemBlocking(@Named("blocking") Set<IndexingItemHandler> handlersSync) {
		this.itemBlocking = handlersSync;
		eventHandlers.addIfAwareAll(handlersSync);
	}
	
	@Inject
	public void setItemBackground(@Named("background") Set<IndexingItemHandler> handlersAsync) {
		this.itemBackground = handlersAsync;
		eventHandlers.addIfAwareAll(handlersAsync);
	}

	@Inject
	public void setItemContentsBufferStrategy(ItemContentBufferStrategy contentsBufferStrategy) {
		this.contentsBufferStrategy = contentsBufferStrategy;
		eventHandlers.addIfAware(contentsBufferStrategy);
	}
	
	@Inject
	public void setItemPropertiesBufferStrategy(ItemPropertiesBufferStrategy propertiesBufferStrategy) {
		this.propertiesBufferStrategy = propertiesBufferStrategy;
		eventHandlers.addIfAware(propertiesBufferStrategy);
	} 
	
	@Inject
	public void setRevisionLookup(@Named("inspection") CmsRepositoryLookup lookup) {
		this.revisionLookup = lookup;
	}

	/**
	 * Optional, adds event listeners that are not added through as other types of dependencies.
	 * 
	 * {@link IndexingEventAware} is detected from the following:
	 * <ul>
	 * <li>{@link #setItemBlocking(Set)}</li>
	 * <li>{@link #setItemBackground(Set)}</li>
	 * <li>{@link #setItemContentsBufferStrategy(ItemContentBufferStrategy)}</li>
	 * <li>{@link #setItemPropertiesBufferStrategy(ItemPropertiesBufferStrategy)}</li>
	 */
	@Inject
	public void addEventListeners(Set<IndexingEventAware> other) {
		if (other != null) {
			this.eventHandlers.addAll(other);
		}
	}
	
	protected Collection<IndexingEventAware> getListerners() {
		return eventHandlers;
	}
	
	/**
	 * 
	 * TODO quite possibly we could realize that single-thread scheduling of handlers is better than different executors,
	 * so we might drop the executors and instead focus on scheduling handlers so that newly added revisions' "blocking"
	 * handlers get inserted before old revision's waiting handlers.
	 * We might also do that up to a limit, so that when a revision has waited for very long we run it anyway.
	 * Conclusion: schedulng should be configurable, not executors.
	 * 
	 * @return
	 */
	protected Executor getExecutorBlocking() {
		return new BlockingExecutor();
	}	
	
	/**
	 * For now we run everything as blocking, to simplify things.
	 * @param handler
	 * @return
	 */
	protected Executor getExecutorBackground() {
		return new BlockingExecutor();
	}
	

	@Override
	public void sync(RepoRevision repoRevision) {
		throw new UnsupportedOperationException("This is the old multirepo service impl");
	}
	
	/**
	 * Polls indexing status, forwards indexing task to {@link #sync(CmsRepositoryInspection, CmsChangesetReader, Iterable)}
	 * 
	 * 
	 */
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
		
		if (!scheduledHighest.containsKey(repository)) {
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
			scheduledHighest.put(repository, ph);
		}
		
		// running may be null if everything is completed
		// TODO find the proper revision dates because those are indexed, needed in SvnTestIndexing too
		List<RepoRevision> range = new LinkedList<RepoRevision>();
		RepoRevision r = scheduledHighest.get(repository);
		if (r == null) {
			logger.debug("No revision status in index. Starting from 0.");
			r = new RepoRevision(0, revisionLookup.getRevisionTimestamp(repository, 0));
		}
		for (long i = r.getNumber(); i <= revision.getNumber(); i++) {
			range.add(new RepoRevision(i, revisionLookup.getRevisionTimestamp(repository, i)));
		}
		logger.debug("Index range: {}", range);
		
		// run
		scheduledHighest.put(repository, revision);
		if (repository instanceof CmsRepositoryInspection) {
			sync((CmsRepositoryInspection) repository, changesetReader, revision, range);
		} else {
			throw new AssertionError("Admin repository instance required for indexing. Got " + repository.getClass());
		}
		
		// end of changeset indexing (i.e. after all background work too)
		// TODO really? yes, but only because all executors are blocking. Must be solved so that 
		
		// Move this to event handler?
		try {
			repositem.commit();
		} catch (SolrServerException e) {
			throw new RuntimeException("error not handled", e);
		} catch (IOException e) {
			throw new RuntimeException("error not handled", e);
		}
	}
	
	/**
	 * Handles indexing status.
	 * @param repository
	 * @param changesets
	 * @param toRevision
	 */
	void sync(CmsRepositoryInspection repository, CmsChangesetReader changesets, RepoRevision reference, Iterable<RepoRevision> range) {
		for (RepoRevision rev : range) {
			if (rev.getNumber() == 0) {
				logger.debug("Revprops indexing for rev 0 not implemented"); // changeset reader couldn't handle 0
				String commitId = repositoryStatus.indexRevStart(repository, rev, null);
				repositoryStatus.indexRevComplete(commitId);
				continue;
			}
			CmsChangeset changeset;
			if (reference.isNewer(rev)) {
				logger.debug("Reading revision {} from repository {}, commit revisions based on {}", rev, repository, reference);
				changeset = changesets.read(repository, rev, reference);
			} else {
				logger.debug("Reading revision {} from repository {}", rev, repository);
				changeset = changesets.read(repository, rev);
			}
			index(repository, changeset);
			// TODO now because all executors are blocking we know that revision is done here, better scheduling insight is needed so we can report event accurately
			eventHandlers.onRevisionComplete(rev);
		}
	}
	
	/**
	 * Runs without validation of status.
	 * @param repository
	 * @param changeset
	 */
	void index(CmsRepository repository, CmsChangeset changeset) {
		logger.info("Indexing revision {} with blocking handlers {} and background handlers {}", new Object[]{changeset.getRevision(), itemBlocking, itemBackground});
		Runnable onComplete = indexRevStart(repository, changeset);
		
		// TODO update existing docs affected by changeset to head=false,
		//  unless in HEAD-referenced changeset mode (typically reindex) where docs get head=false at first indexing
		
		// we may want to extract an item visitor pattern from indexing to generic hook processing
		List<CmsChangesetItem> items = changeset.getItems();
		for (final CmsChangesetItem item : items) {
			logger.debug("Indexing item {}", item);
			indexItemVisit(repository, changeset.getRevision(), item);
			
		}
		// TODO as long as our only executor is #getExecutorBlocking() we can run the end handler here
		// when we have background we need to run it at the end of each changeset, after all items have completed in background
		// This means we need to pass the executors to the item visit above
		onComplete.run();
		
		// guess we need different commit strategies for resync and post-commit indexing, but lets commit every revision for now
		new CoreCommitRevcomplete(repositem).onCompleteRevision();
		
		// guess we also need an optimize strategy
	}
	
	/**
	 * Alternative from our local changeset iteration.
	 * @param progress
	 * 
	 */
	void indexItemVisit(CmsRepository repository, RepoRevision revision, CmsChangesetItem item) {
		
		
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		
		IndexingItemProgressPhases progress = new IndexingItemProgressPhases(repository, revision, item, doc);
		
		// Only use head flag on files for now because we don't have the revision to make the update safely on folders
		if (item.isFile()) {
			if (!item.isAdd()) {
				indexItemMarkPrevious(repository, revision, item);
			}
			// TODO with HEAD reference we could index as non-head immediately, see CmsChangesetReader#read(CmsRepositoryInspection, RepoRevision, RepoRevision) and CmsChangesetItem#isOverwritten()
			doc.addField("head", item.isDelete() ? false : true);
		}

		if (item.isDelete()) {
			progress.setProperties(new ItemPropertiesDeleted());
			progress.setContents(new ItemContentBufferDeleted());
		} else {
			CmsRepositoryInspection repositoryInsp = (CmsRepositoryInspection) repository;
			progress.setProperties(propertiesBufferStrategy.getProperties(repositoryInsp, revision, item.getPath()));
			
			// TODO by setting contents here we do NOT limit access to the background phase, meaning that buffers may live for very long during high indexing load for example reindexing
			// - on the other hand current strategy for tests is to run everything in blocking phase, so could we instead make the background phase synchronous there?
			if (item.isFile()) {
				// should we cast further down instead?
				progress.setContents(contentsBufferStrategy.getBuffer(repositoryInsp, revision, item.getPath(), doc));
			} else {
				progress.setContents(new ItemContentBufferFolder());
			}
		}
		
		Executor blocking = getExecutorBlocking();
		indexItemProcess(blocking, progress, itemBlocking);
		repositoryStatus.solrAdd(doc.getSolrDoc());
		
		progress.setPhase(Phase.update);
		// for simplicity, continue using the same executor service
		// we need status reporting before we start fiddling with background
		// Note that onComplete needs to run after each changeset if we start running in background
		// This method should probably move back to index
		indexItemProcess(blocking, progress, itemBackground);
		if (doc.size() > 0) {
			repositoryStatus.solrAdd(doc.getSolrDoc());
		}
		// TODO run the end handler after all items
	}
	
	protected void indexItemMarkPrevious(CmsRepository repository, RepoRevision revision, CmsChangesetItem item) {
		repositoryStatus.indexItemMarkPrevious(repository, revision, item);
	}
	
	/**
	 * Runs all indexing handlers in a phase using the same executor service.
	 * @param executor {@link ExecutorService}
	 * @param progress 
	 * @param handler
	 */
	void indexItemProcess(Executor executor, IndexingItemProgress progress, Iterable<IndexingItemHandler> handlers) {
		for (IndexingItemHandler handler : handlers) {
			Runnable r = new IndexingItemHandlerRunnable(handler, progress);
			executor.execute(r);
		}		
	}
	
	/**
	 * Index only revprops and only for a single revision; helper to avoid reindex after revprops.
	 * @param repository
	 * @param changeset
	 */
	public void indexRevprops(CmsRepositoryInspection repository, RepoRevision revision) {
		CmsChangeset changeset = changesetReader.read(repository, revision);
		Runnable onComplete = indexRevStart(repository, changeset);
		onComplete.run();
	}

	/**
	 * Index a revision and end with a complete=false status so that items can be indexed.
	 * @param repository
	 * @param changeset
	 * @return task to execute when all indexing for this revision is completed
	 */
	Runnable indexRevStart(CmsRepository repository, CmsChangeset changeset) {
		RepoRevision revision = changeset.getRevision();
		String commitId = repositoryStatus.indexRevStart(repository, revision, null);
		return new RunRevComplete(commitId);
	}
	
	/* moved to ItemPathinfo
	String getId(CmsRepository repository, RepoRevision revision, CmsItemPath path) {
		return repository.getHost() + repository.getUrlAtHost() + (path == null ? "" : path) + "@" + getIdRevision(revision); 
	}
	*/

	String getIdRepository(CmsRepository repository) {
		return repository.getHost() + repository.getUrlAtHost() + "#";
	}	
	
	String getIdCommit(CmsRepository repository, RepoRevision revision) {
		return getIdRepository(repository) + getIdRevision(revision);
	}
	
	private String getIdRevision(RepoRevision revision) {
		return Long.toString(revision.getNumber());
	}
	
	@Override
	public RepoRevision getRevComplete(CmsRepository repository) {
		return scheduledHighest.get(repository); // not handling progress for now
	}

	@Override
	public RepoRevision getRevProgress(CmsRepository repo) {
		return null;
	}

	class RunRevComplete implements Runnable {
		
		private String id;
		
		private RunRevComplete(String id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			repositoryStatus.indexRevComplete(id);
		}
		
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
