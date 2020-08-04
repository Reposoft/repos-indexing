/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrDelete;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Add item to indexing with head=true and 'id' without revision. 
 * Modify progress fields to represent item with head=false for processing in subsequent handlers.
 */
@Singleton // 
public class HandlerHeadClone implements IndexingItemHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	

	private IdStrategy idStrategy;
	private SolrClient repositem;
	private Map<CmsItemPath, RepoRevision> earlierMarkedOverwritten = new HashMap<CmsItemPath, RepoRevision>();
	
	@Inject
	public HandlerHeadClone(CmsRepository repository) {
	}
	
	@Inject
	public void setSolrClient(@Named("repositem") SolrClient repositem) {
		this.repositem = repositem;
	}
	
	@Inject
	public void setIdStrategy(IdStrategy idStrategy) {
		this.idStrategy = idStrategy;
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem item = progress.getItem();
		// Attempt to handle Folders identical to Files.
		
		IndexingDoc fields = progress.getFields();
		String id = (String) fields.getFieldValue("id");
		String idHead = (String) fields.getFieldValue("idhead");
		
		if (id == null) {
			throw new IllegalStateException("Field 'id' must be set by preceeding handler.");
		}
		if (idHead == null) {
			throw new IllegalStateException("Field 'idhead' must be set by preceeding handler.");
		}
		
		if (item.isDelete()) {
			// Remove the head item from indexing.
			// TODO: Verify if fields need to be suppressed. Perhaps not since content is empty? Props empty as well?
			
			new SolrDelete(repositem, idHead).run();
			logger.debug("Removing head=true item on deleted path: {}", item.getPath());
		} else {
			
			fields.setField("id", idHead);
			fields.setField("head", true);
			// TODO: Consider removing idHead before sending head=true item.
			new SolrAdd(repositem, fields).run();
			
			// Removing idHead field because subsequent handlers processes the head=false item.
			fields.removeField("idhead");
			// Restore the id with revision.
			fields.setField("id", id);
			
		}
		// Flag 'head' is always false when returning, subsequent handlers will add the item representing history.
		fields.setField("head", false);
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}


}
