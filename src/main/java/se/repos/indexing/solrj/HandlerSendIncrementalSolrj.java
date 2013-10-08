package se.repos.indexing.solrj;

import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

/**
 * Marks a point where item should be sent to index if there is a risk that the rest of the handler chain will wait for other jobs.
 */
public class HandlerSendIncrementalSolrj implements IndexingItemHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private SolrServer solr;

	public HandlerSendIncrementalSolrj(SolrServer solrCore) {
		this.solr = solrCore;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		logger.warn("Incremental send not implemented, at {}", progress);
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

}
