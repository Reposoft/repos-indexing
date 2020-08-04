/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrDelete;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

/**
 * Add item to indexing with head=true and 'id' without revision. 
 * Modify progress fields to represent item with head=false for processing in subsequent handlers.
 */
@Singleton // 
public class HandlerHeadClone implements IndexingItemHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private SolrClient repositem;
	
	@Inject
	public HandlerHeadClone(CmsRepository repository) {
		// No need for repository at this time.
	}
	
	@Inject
	public void setSolrClient(@Named("repositem") SolrClient repositem) {
		this.repositem = repositem;
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
		} else if (item.isOverwritten() && item.isFile()) {
			// Overwritten detection is not stable for Folders.
			// This optimization can have interesting effects (missing head item) in combination with indexing-mode = none.
			// It is assumed that items suppressed by indexing-mode should be suppressed from head or iterated after the suppressed revision.
			logger.trace("Suppressing head indexing for overwritten object: {}", item.getPath());
		} else {
			// Set ID to idHead for the purpose of adding the item representing latest.
			fields.setField("id", idHead);
			fields.setField("head", true);
			new SolrAdd(repositem, fields).run();
			
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
