/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Date;

import org.junit.Test;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

public class ReposIndexingImplTest {

	@Test
	public void testGetId() {
		CmsRepository repo = new CmsRepository("http://host.name/svn/repo");
		RepoRevision rev = new RepoRevision(123, new Date(123456));
		ReposIndexingImpl impl = new ReposIndexingImpl();
		// always index hostname, useful for resolving URLs
		// don't do a root marker etc, there'll be repo fileds for parent, name etc
		// use numeric revision if available, shorter and better uniqueness
		// TODO move to IdStrategy impl assertEquals("host.name/svn/repo/dir@123", impl.getId(repo, rev, new CmsItemPath("/dir")));
		// TODO move to IdStrategy impl assertEquals("repo root", "host.name/svn/repo@123", impl.getId(repo, rev, null));
		assertEquals("commit ids should not be confused with root items", "host.name/svn/repo#123", impl.getIdCommit(repo, rev));
		assertEquals("repository ids are not used directly but useful for query on commit status", "host.name/svn/repo#", impl.getIdRepository(repo));
	}
	
	@Test
	public void testDontMarkPreviousAtAdd() {
		CmsRepositoryInspection repo = new CmsRepositoryInspection("http://host.name/svn/repo", new File("/tmp/repo"));
		RepoRevision rev = new RepoRevision(123, new Date(123456));
		ReposIndexingImpl impl = new ReposIndexingImpl();
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getRevisionObsoleted()).thenThrow(new AssertionError("For add operation there should be no check for previous revision"));
		when(item.isAdd()).thenReturn(true);
		when(item.isFile()).thenReturn(true);
		try {
			impl.indexItemVisit(repo, rev, item);
		} catch (NullPointerException e) {
			// expected
		}
		verify(item).isAdd();
	}
	
	@Test
	public void testRevisionNotification() {
		ReposIndexingImpl impl = new ReposIndexingImpl();
		
	}
	
	@Test
	public void testHandlerException() {
		
	}
	
	@Test
	public void testHandlerDependencyVerification() {
		
	}
	
	@Test
	public void testDeepCopyRelevantFields() {
		
	}
	
}
