/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrDelete extends SolrOp<UpdateResponse> {
	
	private static final Logger logger = LoggerFactory.getLogger(SolrDelete.class);
	
	private List<String> ids;
	
	public SolrDelete(SolrClient core, String id) {
		super(core);
		this.ids = Arrays.asList(id);
	}
	
	public SolrDelete(SolrClient core, List<String> ids) {
		super(core);
		this.ids = ids;
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		UpdateResponse delete = core.deleteById(ids);
		logger.debug("Delete response: {}", delete);
		doLogSlowQuery(core, "deleteById", "(no of IDs: " + ids.size() + ")" + ids.get(0), delete);
		return delete;
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Allowing retry for delete operations. 
		return true;
	}

}
