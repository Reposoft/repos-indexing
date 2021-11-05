/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;

import se.repos.indexing.IndexingDoc;

public class SolrAddCommitWithin extends SolrAdd {

	private int commitWithin = 500;
	
	public SolrAddCommitWithin(SolrClient core, IndexingDoc doc) {
		super(core, doc);
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		UpdateRequest req = new UpdateRequest();
		req.setCommitWithin(commitWithin);
		req.add(documents);
		NamedList<Object> responseValues = core.request(req);
		UpdateResponse response = new UpdateResponse();
		response.setResponse(responseValues);
		doLogSlowQuery(core, "add", Integer.toString(documents.size()), response);
		return response;
	}

	public int getCommitWithin() {
		return commitWithin;
	}

	/**
	 * @param commitWithin time in ms before SolR performs commit.
	 */
	public void setCommitWithin(int commitWithin) {
		this.commitWithin = commitWithin;
	}
	

}
