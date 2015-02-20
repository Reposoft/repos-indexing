/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;

import se.repos.indexing.IndexAdmin;
import se.repos.indexing.IndexingHandlers;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.repos.indexing.scheduling.IndexingScheduleBlockingOnly;
import se.repos.indexing.twophases.ItemContentsMemory;
import se.repos.indexing.twophases.ItemPropertiesImmediate;
import se.simonsoft.cms.backend.svnkit.CmsRepositorySvn;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLookRepo;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLookRepo;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsRepositoryLookupSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CommitRevisionCache;
import se.simonsoft.cms.backend.svnkit.svnlook.CommitRevisionCacheDefault;
import se.simonsoft.cms.backend.svnkit.svnlook.SvnlookClientProviderStateless;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.testing.svn.CmsTestRepository;
import se.simonsoft.cms.testing.svn.SvnTestSetup;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class ReposIndexingPerRepositoryIntegrationTest {

	// reuse solr instance dir
	private final File instanceDir = new File(System.getProperty("java.io.tmpdir") + "/solr-" + this.getClass().getSimpleName());
	private EmbeddedSolrServer forTearDown = null;
	
	private Injector context = null;
	
	@Before
	public void setUp() throws Exception {
		final SolrServer repositem = setUpSolrRepositem();
		
		final CmsTestRepository repository = SvnTestSetup.getInstance().getRepository();
		
		Module backend = new AbstractModule() { @Override protected void configure() {
			CmsRepositorySvn configRepository = CmsRepositorySvn.fromTesting(repository);
			bind(CmsRepository.class).toInstance(configRepository);
			bind(CmsRepositorySvn.class).toInstance(configRepository);
			bind(CmsTestRepository.class).toInstance(repository); // should there really be services that expect this type?
			
			bind(CmsChangesetReader.class).to(CmsChangesetReaderSvnkitLookRepo.class);
			bind(CmsContentsReader.class).to(CmsContentsReaderSvnkitLookRepo.class);
			bind(CommitRevisionCache.class).to(CommitRevisionCacheDefault.class);
			bind(CmsRepositoryLookup.class).annotatedWith(Names.named("inspection")).to(CmsRepositoryLookupSvnkitLook.class);
			bind(SVNLookClient.class).toProvider(SvnlookClientProviderStateless.class);
		}};
		
		Module indexing = new AbstractModule() { @Override protected void configure() {
			bind(SolrServer.class).annotatedWith(Names.named("repositem")).toInstance(repositem);
			bind(ReposIndexing.class).to(ReposIndexingPerRepository.class);
			bind(IndexingSchedule.class).to(IndexingScheduleBlockingOnly.class);
			bind(IndexAdmin.class).to(IndexAdminPerRepositoryRepositem.class);
			
			Multibinder<IndexingItemHandler> handlers = Multibinder.newSetBinder(binder(), IndexingItemHandler.class);
			IndexingHandlers.configureFirst(handlers);
			// any custom handlers go here
			IndexingHandlers.configureLast(handlers);
			
			bind(IdStrategy.class).to(IdStrategyDefault.class);
			bind(ItemContentBufferStrategy.class).to(ItemContentsMemory.class); // we shuld have a memory-only impl
			bind(ItemPropertiesBufferStrategy.class).to(ItemPropertiesImmediate.class);
		}};
		
		context = Guice.createInjector(backend, indexing);
	}

	private SolrServer setUpSolrRepositem() throws IOException {
		File coreSource = new File("src/main/resources/se/repos/indexing/solr/repositem/");
		
		if (instanceDir.exists()) { // instance dir is kept for inspection after each test but recreated before each new test
			FileUtils.deleteDirectory(instanceDir);
		}

		String coreName = "repositem";
		FileUtils.copyDirectory(coreSource, new File(instanceDir, coreName));
		FileUtils.copyFile(new File(coreSource.getParentFile(),  "testing-home/solr.xml"), new File(instanceDir, "solr.xml"));
		
		SolrResourceLoader solrResourceLoader = new SolrResourceLoader(instanceDir.getAbsolutePath());
		CoreContainer solrCoreContainer = new CoreContainer(solrResourceLoader);
		solrCoreContainer.load();
		final SolrServer repositem = new EmbeddedSolrServer(solrCoreContainer, "repositem");
		forTearDown = (EmbeddedSolrServer) repositem;
		return repositem;
	};
	
	private void tearDownSolrRepositem() {
		forTearDown.shutdown();
		
		new File(instanceDir, "/repositem/core.properties").delete(); // Solr 4.5.0 won't load the core if core.properties is present
		System.out.println("Load test core using:");
		String loadName = "repositem-" + instanceDir.getName();
		String corePath = instanceDir.getAbsolutePath() + "/repositem";
		System.out.println("http://localhost:8983/solr/admin/cores?action=RELOAD&name=" + loadName);
		System.out.println("http://localhost:8983/solr/admin/cores?action=CREATE&name=" + loadName + "&instanceDir=" + corePath);// + "&dataDir=" + corePath + "/data");		
	}
	
	@After
	public void tearDown() throws Exception {
		SvnTestSetup.getInstance().tearDown();
		
		tearDownSolrRepositem();
	}
	
	@Test
	public void testMarkItemHead() throws SolrServerException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r3.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		indexing.sync(new RepoRevision(1, new Date(1)));
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		SolrDocumentList r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		assertEquals(3, r1.size());
		assertEquals("/dir", r1.get(0).getFieldValue("path"));
		for (int i = 0; i < 3; i++) {
			if ("folder".equals(r1.get(i).get("type"))) {
				continue; // TODO use lookup on path + head=true to get historical folder revision and start marking head again
			}
			assertEquals("at " + r1.get(i).get("path"), true, r1.get(i).get("head"));
		}
		
		indexing.sync(new RepoRevision(2, new Date(2)));
		SolrDocumentList r2r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		// TODO support folders assertEquals("/dir " + r2r1.get(0), true, r2r1.get(0).get("head"));
		assertEquals("/dir/t2.txt " + r2r1.get(1), true, r2r1.get(1).get("head"));
		assertEquals("should have updated old /t1.txt" + r2r1.get(2), false, r2r1.get(2).get("head"));
		SolrDocumentList r2 = repositem.query(new SolrQuery("id:*@0000000002").setSort("path", ORDER.asc)).getResults();
		assertEquals("next revision should be head, " + r2.get(0), true, r2.get(0).get("head"));
		
		indexing.sync(new RepoRevision(3, new Date(3)));
		// everything from r1 should now have been replaced with later versions
		SolrDocumentList r3r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("/dir", r3r1.get(0).get("path"));
		assertEquals("/dir/t2.txt", r3r1.get(1).get("path"));
		assertEquals("A", r3r1.get(1).get("pathstat"));
		assertEquals("/t1.txt", r3r1.get(2).get("path"));
		assertEquals("Should have revauthor.", "solsson", r3r1.get(2).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "Two files with two lines each", r3r1.get(2).getFieldValue("revcomment"));
		assertEquals("Revision 1 had only these files, nothing else should have been indexed on rev 1 since then", 3, r3r1.size());

		// TODO support folders assertEquals("Folder is deleted and thus no longer in head", false, r3r1.get(0).get("head"));
		assertEquals("Old file that is now gone because of folder delete should not be head", false, r3r1.get(1).get("head"));
		assertEquals("The file that was changed in r3 should now be marked as non-head", false, r3r1.get(2).get("head"));
		
		SolrDocumentList r3r2 = repositem.query(new SolrQuery("id:*@0000000002").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was only a file edit in rev 2", 1, r3r2.size());
		assertEquals("/t1.txt", r3r2.get(0).get("path"));
		assertEquals("Should have revauthor.", "test", r3r2.get(0).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "file modification", r3r2.get(0).getFieldValue("revcomment"));
		assertEquals("Rev 2 is still HEAD for this file", true, r3r2.get(0).get("head"));
		
		SolrDocumentList r3r3 = repositem.query(new SolrQuery("id:*@0000000003").setSort("path", ORDER.asc)).getResults();
		assertEquals("Deletions should be indexed so we know when an item disappeared", "/dir", r3r3.get(0).get("path"));
		assertEquals("Should have revauthor.", "test", r3r3.get(0).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "folder move without changes to the contained file", r3r3.get(0).getFieldValue("revcomment"));
		assertEquals("Should have revcauthor when not 'derived' from folder copy", "test", r3r3.get(0).getFieldValue("revcauthor"));
		assertEquals("Should have revccomment when not 'derived' from folder copy", "folder move without changes to the contained file", r3r3.get(0).getFieldValue("revccomment"));		
		// TODO assertEquals("Deletions should always be !head", false, r3r3.get(0).get("head"));
		assertEquals("Deletions should always be !head", false, r3r3.get(1).get("head"));
		assertEquals("Derived delete", "/dir/t2.txt", r3r3.get(1).get("path"));
		assertEquals(false, r3r3.get(1).get("head"));
		assertEquals("Folder copy", "/dir2", r3r3.get(2).get("path"));
		// TODO assertEquals("This revision is HEAD", true, r3r3.get(2).get("head"));
		assertEquals("Derived", "/dir2/t2.txt", r3r3.get(3).get("path"));
		assertEquals(true, r3r3.get(3).get("head"));
		
		SolrDocumentList r4 = repositem.query(new SolrQuery("type:commit")).getResults();
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.log"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.author"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.date"));
		
		assertEquals("Two files with two lines each", r4.get(1).getFieldValue("proprev_svn.log"));
		assertEquals("solsson", r4.get(1).getFieldValue("proprev_svn.author"));
		assertEquals("2012-09-27T12:05:34.040515Z", r4.get(1).getFieldValue("proprev_svn.date"));
		
		assertEquals("file modification", r4.get(2).getFieldValue("proprev_svn.log"));
		assertEquals("test", r4.get(2).getFieldValue("proprev_svn.author"));
		assertEquals("2013-03-21T19:16:28.271167Z", r4.get(2).getFieldValue("proprev_svn.date"));
		
		assertEquals("folder move without changes to the contained file", r4.get(3).getFieldValue("proprev_svn.log"));
		assertEquals("test", r4.get(3).getFieldValue("proprev_svn.author"));
		assertEquals("2013-03-21T19:16:42.295071Z", r4.get(3).getFieldValue("proprev_svn.date"));
		
		
		// TODO we could propedit on dir2 and check that rev 3 of it becomes !head
		
		// TODO if we now modify t2 then latest dir2 should still be head
		
		// TODO if we then delete dir2 in the next commit we can demonstrate the issue with marking folders as !head when files have changed in them; need for workaround
	}	

	
	@Test
	public void testMarkItemHeadCopy() throws SolrServerException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r5-copy.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		// Confirmed that each revision is indexed with r5 as reference revision.
		indexing.sync(new RepoRevision(5, new Date(5)));
		
		// Test that we can reindex without failure.
		
		SolrDocumentList r4r4 = repositem.query(new SolrQuery("id:*@0000000004").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("There was a folder and a file rev 4", 2, r4r4.size());
		
		// TODO: Assert on copied item.
	}
	
	@Test
	public void testMarkItemHeadCopyDeleted() throws SolrServerException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r6-copydeleted.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		// Confirmed that each revision is indexed with r5 as reference revision.
		indexing.sync(new RepoRevision(6, new Date(6)));
				
		// Test that we can reindex without failure.
		System.out.println("Test that we can reindex without failure.");
		
		SolrDocumentList r6r6 = repositem.query(new SolrQuery("id:*@0000000006").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("Restored a folder and two files in rev 6", 3, r6r6.size());
		
		// TODO: Assert on restored items.
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testAbortedRev() throws SolrServerException, IOException {
		
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r3.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		CmsChangesetReader changesetReader = spy(context.getInstance(CmsChangesetReader.class));
		((ReposIndexingPerRepository) indexing).setCmsChangesetReader(changesetReader);
		
		RepoRevision revision1 = RepoRevision.parse("1/2012-09-27T12:05:34.040515Z");
		RepoRevision revision2 = RepoRevision.parse("2/2013-03-21T19:16:28.271Z");
		RepoRevision revision3 = RepoRevision.parse("3/2013-03-21T19:16:42.295Z");
		
		// first indexing, two revisions in one sync
		indexing.sync(revision2);
		assertEquals("should have indexed up to the given revision", 2, indexing.getRevision().getNumber());
		QueryResponse r1 = repositem.query(new SolrQuery("type:commit").addSort("rev", ORDER.asc));
		assertEquals("Rev 0 should have been indexed in addition to 1 and 2", 3, r1.getResults().size());
		assertEquals("Rev 0 should be marked as completed", true, r1.getResults().get(0).getFieldValue("complete"));
		
		// second indexing
		indexing.sync(revision3);
		assertEquals("Revision 3 should have been indexed", 1,
				repositem.query(new SolrQuery("type:commit AND rev:3 AND complete:true")).getResults().getNumFound());
		
		// new indexing service, recover sync status
		ReposIndexing indexing2 = context.getInstance(ReposIndexing.class);
		((ReposIndexingPerRepository) indexing2).setCmsChangesetReader(changesetReader);
		indexing2.sync(revision3); // same revision as before, because polling is done at sync
		assertNotNull("New indexing should poll for indexed revision",
				indexing2.getRevision());
		assertEquals("New indexing should poll for highest indexed (started) revision", 
				3, indexing2.getRevision().getNumber());
		
		// mess with the index to see how sync status is handled
		SolrInputDocument markAsFailed = new SolrInputDocument();
		markAsFailed.setField("id", r1.getResults().get(1).getFieldValue("id").toString().replace("#0000000001", "#0000000002"));
		markAsFailed.setField("complete", new HashMap<String, Boolean>() {{
			put("set", false);
		}});
		repositem.add(markAsFailed);
		repositem.commit();
		assertEquals("Service isn't required (or expected) to poll again, can assume no cuncurrent indexing",
				3, indexing.getRevision().getNumber());
		
		// index after incomplete (though normally rev 3 wouldn't exist if rev 2 is incomplete)
		ReposIndexing indexing3 = context.getInstance(ReposIndexing.class);
		((ReposIndexingPerRepository) indexing3).setCmsChangesetReader(changesetReader);
		try {
			indexing3.sync(revision3);
			fail("Should throw exception because this is an index state that our code should never be able to produce, as it is expected to abort on any error");
		} catch (IllegalStateException e) {
			// expected
		}
		
		markAsFailed.setField("id", r1.getResults().get(1).getFieldValue("id").toString().replace("#0000000001", "#0000000003"));
		markAsFailed.setField("complete", new HashMap<String, Boolean>() {{
			put("set", false);
		}});
		repositem.add(markAsFailed);
		repositem.commit();
		indexing3.sync(revision3);
		assertEquals("Revision 2 and 3 should have been indexed", 4,
				repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().getNumFound());
		
		verify(changesetReader, times(1)).read(revision1, revision2); // first
		verify(changesetReader, times(1)).read(revision2); // first
		verify(changesetReader, times(1)).read(revision2, revision3); // after recover
		verify(changesetReader, times(2)).read(revision3); // second, no read needed for third, read again after recover
		
		// checking with verify is difficult, can also be done with capture
		ArgumentCaptor<RepoRevision> revsAlone = ArgumentCaptor.forClass(RepoRevision.class);
		verify(changesetReader, times(3)).read(revsAlone.capture());
		ArgumentCaptor<RepoRevision> revsWith = ArgumentCaptor.forClass(RepoRevision.class);
		ArgumentCaptor<RepoRevision> revsRef = ArgumentCaptor.forClass(RepoRevision.class);
		verify(changesetReader, times(2)).read(revsWith.capture(), revsRef.capture());
		assertEquals(revision1, revsWith.getAllValues().get(0));
		assertEquals(revision2, revsRef.getAllValues().get(0));
		assertEquals(revision2, revsAlone.getAllValues().get(0));
		assertEquals(revision3, revsAlone.getAllValues().get(1));
		assertEquals(revision2, revsWith.getAllValues().get(1));
		assertEquals(revision3, revsRef.getAllValues().get(1));
		assertEquals(revision3, revsAlone.getAllValues().get(2));
	}
	
	@Test
	public void testSyncTwice() throws SolrServerException, IOException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		indexing.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		QueryResponse r1 = repositem.query(new SolrQuery("type:commit AND rev:1"));
		assertEquals(true, r1.getResults().get(0).getFieldValue("complete"));
		
		ReposIndexing indexing2 = context.getInstance(ReposIndexing.class);
		assertTrue("This test is uninteresting if context has a singleton", indexing2 != indexing);
		indexing2.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		repositem.commit(); // to be sure that second sync doesn't do any solr operations
		
		QueryResponse r2 = repositem.query(new SolrQuery("type:commit AND rev:1"));
		assertEquals(true, r2.getResults().get(0).getFieldValue("complete"));
	}

	@Test
	public void testSyncHighLow() throws SolrServerException, IOException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r3.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		indexing.sync(RepoRevision.parse("2/2013-03-21T19:16:28.271Z"));
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		assertEquals(3, repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().size());
		
		ReposIndexing indexing2 = context.getInstance(ReposIndexing.class);
		assertTrue("This test is uninteresting if context has a singleton", indexing2 != indexing);
		indexing2.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		repositem.commit(); // to be sure that second sync doesn't do any solr operations
		
		assertEquals(3, repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().size());
	}	
	
	@Test
	public void testIndexingModeNone() throws SolrServerException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r3-indexing-mode-none.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		indexing.sync(RepoRevision.parse("3/2013-03-21T19:16:42.295Z"));
		assertEquals("should have indexed up to the given revision", 3, indexing.getRevision().getNumber());
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		assertEquals("should have indexed rev 1", 4, repositem.query(new SolrQuery("rev:1")).getResults().size());
		assertEquals("should only index the commit for rev 2", 1, repositem.query(new SolrQuery("rev:2")).getResults().size());
		assertEquals("should have indexed rev 3", 5, repositem.query(new SolrQuery("rev:3")).getResults().size());
		
	}
	
}
