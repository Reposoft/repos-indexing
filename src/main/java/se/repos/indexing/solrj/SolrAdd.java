/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class SolrAdd extends SolrOp<UpdateResponse> {

	private SolrInputDocument doc;

	public SolrAdd(SolrClient core, IndexingDoc doc) {
		super(core);
		if (doc instanceof IndexingDocIncrementalSolrj) {
			this.doc = ((IndexingDocIncrementalSolrj) doc).getSolrDoc();
		} else {
			throw new IllegalArgumentException("Unrecognized fields type " + doc.getClass());
		}
	}
	
	public SolrAdd(SolrClient core, SolrInputDocument doc) {
		super(core);
		this.doc = doc;
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		return core.add(doc);
	}

}
