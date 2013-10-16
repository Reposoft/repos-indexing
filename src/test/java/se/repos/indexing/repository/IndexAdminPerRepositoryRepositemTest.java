/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexAdmin;
import se.repos.indexing.item.IdStrategyDefault;
import se.simonsoft.cms.item.CmsRepository;

public class IndexAdminPerRepositoryRepositemTest {

	@Test
	public void test() throws SolrServerException, IOException {
		CmsRepository repository = new CmsRepository("http://localhost:1234/svn/r");
		IdStrategy idStrategy = new IdStrategyDefault();
		SolrServer repositem = mock(SolrServer.class);
		
		IndexAdmin indexAdmin = new IndexAdminPerRepositoryRepositem(repository, idStrategy, repositem);
		final List<Object> calls = new LinkedList<Object>();
		indexAdmin.addPostAction(new IndexAdmin() {
			@Override
			public void clear() {
				calls.add(null);
			}
			@Override
			public void addPostAction(IndexAdmin notificationReceiver) {
				fail("Should not be called for receivers");
			}
		});
		
		indexAdmin.clear();
		
		verify(repositem).deleteByQuery("repoid:\"localhost:1234/svn/r\"", 0);
		assertEquals("Should notify", 1, calls.size());
	}

}
