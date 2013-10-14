/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.*;

import org.apache.log4j.spi.RepositorySelector;
import org.junit.Test;

import se.repos.indexing.IndexingState;
import se.repos.indexing.ReposIndexing;
import se.simonsoft.cms.item.CmsRepository;

public class ReposIndexingPerRepositoryTest {

	@Test
	public void testSyncRepoRevision() {
		CmsRepository repository = new CmsRepository("http://testing/svn/repo");
		ReposIndexing indexing1 = new ReposIndexingPerRepository(repository);
		ReposIndexing indexing2 = new ReposIndexingPerRepository(repository);
		
		// TODO how to make a threaded test that stops indexin1 inside status check and runs indexing2?
	}

}
