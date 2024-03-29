/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class SolrAdd extends SolrOp<UpdateResponse> {

	protected Collection<SolrInputDocument> documents;

	public SolrAdd(SolrClient core, IndexingDoc doc) {
		super(core);
		if (doc instanceof IndexingDocIncrementalSolrj) {
			this.documents = Arrays.asList(((IndexingDocIncrementalSolrj) doc).getSolrDoc());
		} else {
			throw new IllegalArgumentException("Unrecognized fields type " + doc.getClass());
		}
	}
	
	public SolrAdd(SolrClient core, SolrInputDocument doc) {
		super(core);
		this.documents = Arrays.asList(doc);
	}

	public SolrAdd(SolrClient core, Collection<SolrInputDocument> documents) {
		super(core);
		this.documents = documents;
	}
	
	

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		UpdateResponse response = core.add(documents);
		doLogSlowQuery(core, "add", Integer.toString(documents.size()), response);
		return response;
	}

	@Override
	protected boolean isRetryAllowed() {
		// Allowing retry for add operations at this time. 
		// Needs more investigation whether a SolR restart here could loose Adds.
		return true;
	}

}
