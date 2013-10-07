package se.repos.indexing.repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexingEventAware;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;

public class ReposIndexingPerRepository implements ReposIndexing {

	private CmsRepository repository;
	private IndexingSchedule schedule;
	private CmsChangesetReader changesetReader;
	private CmsRepositoryLookup revisionLookup;
	private Set<IndexingItemHandler> handlers;

	@Inject
	public ReposIndexingPerRepository(CmsRepository repository) {
		this.repository = repository;
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
		// TODO Auto-generated method stub

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
