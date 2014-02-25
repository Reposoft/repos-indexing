/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.Test;

import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class ReposIndexingPerRepositoryTest {

	@Test
	public void testSyncRepoRevision() {
		CmsRepository repository = new CmsRepository("http://testing/svn/repo");
		ReposIndexing indexing1 = new ReposIndexingPerRepository(repository);
		ReposIndexing indexing2 = new ReposIndexingPerRepository(repository);
		
		// TODO how to make a threaded test that stops indexin1 inside status check and runs indexing2?
	}
	
	@Test
	public void testClear() {
		CmsRepository repository = new CmsRepository("http://testing/svn/repo");
		ReposIndexingPerRepository indexing = new ReposIndexingPerRepository(repository);
		
		IndexAdminPerRepositoryRepositem admin = new IndexAdminPerRepositoryRepositem(repository, new IdStrategyDefault(), mock(SolrServer.class));
		indexing.setIndexAdmin(admin);
		
		IndexingSchedule schedule = mock(IndexingSchedule.class);
		when(schedule.isComplete()).thenReturn(true).thenReturn(false);
		indexing.setIndexingSchedule(schedule);
		admin.clear();
		
		verify(schedule).isComplete(); // indicates that we ran the code that clears state
		
		try {
			admin.clear();
			// log a warning or throw exception when schedule is running?
		} catch (Exception e) {
			fail("Currently we've decided to only log a warning when clearing while schedule is still running");
		}
	}

}
