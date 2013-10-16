package se.repos.indexing.repository;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexAdmin;
import se.repos.indexing.solrj.SolrDelete;
import se.simonsoft.cms.item.CmsRepository;

public class IndexAdminPerRepositoryRepositem implements IndexAdmin {

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
		new SolrDelete(repositem, query).run();
		for (IndexAdmin p : postActions) {
			p.clear();
		}
	}

}
