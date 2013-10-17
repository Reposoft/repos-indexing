/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexAdmin;
import se.repos.indexing.solrj.SolrCommit;
import se.repos.indexing.solrj.SolrDelete;
import se.simonsoft.cms.item.CmsRepository;

@Singleton // Notification receivers won't be notified if this isn't a singleton
public class IndexAdminPerRepositoryRepositem implements IndexAdmin {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private CmsRepository repository;
	private IdStrategy idStrategy;
	private SolrServer repositem;
	private List<IndexAdmin> postActions = new LinkedList<IndexAdmin>();
	
	@Inject
	public IndexAdminPerRepositoryRepositem(CmsRepository repository,
			IdStrategy idStrategy,
			@Named("repositem") SolrServer repositem) {
		this.repository = repository;
		this.idStrategy = idStrategy;
		this.repositem = repositem;
	}
	
	@Override
	public void addPostAction(IndexAdmin notificationReceiver) {
		this.postActions.add(notificationReceiver);
	}

	@Override
	public void clear() {
		String query = "repoid:\"" + idStrategy.getIdRepository(repository).replace("\"", "\\\"") + '"';
		logger.info("Clearing repository {} using query {}", repository, query);
		new SolrDelete(repositem, query).run();
		new SolrCommit(repositem).run();
		for (IndexAdmin p : postActions) {
			p.clear();
		}
	}

}
