/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

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
import se.repos.restclient.RestAuthentication;
import se.repos.restclient.auth.RestAuthenticationSimple;
import se.simonsoft.cms.backend.svnkit.CmsRepositorySvn;
import se.simonsoft.cms.backend.svnkit.config.SvnKitAuthManagerProvider;
import se.simonsoft.cms.backend.svnkit.config.SvnKitLowLevelProvider;
import se.simonsoft.cms.backend.svnkit.info.CmsRepositoryLookupSvnkit;
import se.simonsoft.cms.backend.svnkit.info.change.CmsChangesetReaderSvnkit;
import se.simonsoft.cms.backend.svnkit.info.change.CmsContentsReaderSvnkit;
import se.simonsoft.cms.backend.svnkit.info.change.CommitRevisionCache;
import se.simonsoft.cms.backend.svnkit.info.change.CommitRevisionCacheRepo;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.testing.svn.CmsTestRepository;
import se.simonsoft.cms.testing.svn.SvnTestSetup;

public class ReposIndexingPerRepositoryIntegrationTest {

	// reuse solr instance dir
	private final File instanceDir = new File(System.getProperty("java.io.tmpdir") + "/solr-" + this.getClass().getSimpleName());
	private EmbeddedSolrServer forTearDown = null;
	
	private Injector context = null;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Before
	public void setUp() throws Exception {
		final SolrClient repositem = setUpSolrRepositem();
		
		final CmsTestRepository repository = SvnTestSetup.getInstance().getRepository();
		
		Module backend = new AbstractModule() { @Override protected void configure() {
			CmsRepositorySvn configRepository = CmsRepositorySvn.fromTesting(repository);
			bind(CmsRepository.class).toInstance(configRepository);
			bind(CmsRepositorySvn.class).toInstance(configRepository);
			bind(CmsTestRepository.class).toInstance(repository); // should there really be services that expect this type?
			
			// Production use will not require Auth if connecting to an Apache without authn/authz.
			// Need a specific SVNKit provider.
			bind(RestAuthentication.class).toInstance(new RestAuthenticationSimple(repository.getAuthenticatedUser(), repository.getAuthenticatedPassword()));
			bind(ISVNAuthenticationManager.class).toProvider(SvnKitAuthManagerProvider.class);
			//bind(SVNClientManager.class).toProvider(SvnKitClientManagerProvider.class);
			
			bind(CmsChangesetReader.class).to(CmsChangesetReaderSvnkit.class);
			bind(CmsContentsReader.class).to(CmsContentsReaderSvnkit.class);
			bind(CommitRevisionCache.class).toInstance(new CommitRevisionCacheRepo()); // Bind an instance of the cache.
			bind(CmsRepositoryLookup.class).annotatedWith(Names.named("inspection")).to(CmsRepositoryLookupSvnkit.class);
			bind(SVNRepository.class).toProvider(SvnKitLowLevelProvider.class);
		}};
		
		Module indexing = new AbstractModule() { @Override protected void configure() {
			bind(SolrClient.class).annotatedWith(Names.named("repositem")).toInstance(repositem);
			bind(ReposIndexing.class).to(ReposIndexingPerRepository.class);
			bind(IndexingSchedule.class).to(IndexingScheduleBlockingOnly.class);
			bind(IndexAdmin.class).to(IndexAdminPerRepositoryRepositem.class);
			
			Multibinder<IndexingItemHandler> handlers = Multibinder.newSetBinder(binder(), IndexingItemHandler.class);
			IndexingHandlers.configureFirst(handlers);
			// any custom handlers go here
			IndexingHandlers.configureLast(handlers);
			
			bind(IdStrategy.class).to(IdStrategyDefault.class);
			//bind(ItemContentBufferStrategy.class).to(ItemContentsStream.class);
			bind(ItemContentBufferStrategy.class).to(ItemContentsMemory.class); // we shuld have a memory-only impl
			bind(ItemPropertiesBufferStrategy.class).to(ItemPropertiesImmediate.class);
		}};
		
		context = Guice.createInjector(backend, indexing);
	}

	private SolrClient setUpSolrRepositem() throws IOException {
		File coreSource = new File("src/main/resources/se/repos/indexing/solr/repositem/");
		
		if (instanceDir.exists()) { // instance dir is kept for inspection after each test but recreated before each new test
			FileUtils.deleteDirectory(instanceDir);
		}

		String coreName = "repositem";
		FileUtils.copyDirectory(coreSource, new File(instanceDir, coreName));
		FileUtils.copyFile(new File(coreSource.getParentFile(),  "testing-home/solr.xml"), new File(instanceDir, "solr.xml"));

		/*
		CoreContainer solrCoreContainer = CoreContainer.createAndLoad(instanceDir.toPath());
		*/
		CoreContainer solrCoreContainer = new CoreContainer(instanceDir.toPath(), null);
		logger.info("Loading SolR container...");
		solrCoreContainer.load();
		logger.info("Loaded SolR container.");
		final SolrClient repositem = new EmbeddedSolrServer(solrCoreContainer, "repositem");
		logger.info("Created EmbeddedSolrServer.");
		forTearDown = (EmbeddedSolrServer) repositem;
		return repositem;
	};
	
	private void tearDownSolrRepositem() throws IOException {
		logger.info("Closing EmbeddedSolrServer...");
		forTearDown.close();
		
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
	
	private String getTail(Object id) {
		
		String str = (String) id;
		String[] split = str.split("/");
		return split[split.length-1];
	}
	
	@Test
	public void testMarkItemHead() throws SolrServerException, IOException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r3.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		indexing.sync(new RepoRevision(1, new Date(1)));
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		// Verify r1 head:false
		SolrDocumentList r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		assertEquals(3, r1.size());
		assertEquals("/dir", r1.get(0).getFieldValue("path"));
		assertAllHeadFalse(r1);
		r1 = null;
		
		// Verify r1 head:true
		SolrDocumentList r1Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r1", 3, r1Head.size());
		assertEquals("folders are also head:true now", "/dir", r1Head.get(0).get("path"));
		assertEquals("...", "/dir/t2.txt", r1Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r1Head.get(2).get("path"));
		assertEquals("...", 1L, r1Head.get(0).get("rev"));
		assertEquals("...", 1L, r1Head.get(1).get("rev"));
		assertEquals("...", 1L, r1Head.get(2).get("rev"));
		assertEquals("...", 1L, r1Head.get(0).get("revc"));
		assertEquals("...", 1L, r1Head.get(1).get("revc"));
		assertEquals("...", 1L, r1Head.get(2).get("revc"));
		
		assertEquals("...", "folder", r1Head.get(0).get("type"));
		assertEquals("...", "file", r1Head.get(1).get("type"));
		assertEquals("...", "file", r1Head.get(2).get("type"));
		
		
		r1Head = null;
		
		// Verify r1 after r2 head:false
		indexing.sync(new RepoRevision(2, new Date(2)));
		SolrDocumentList r2r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		assertEquals(3, r2r1.size());
		assertAllHeadFalse(r2r1);
		r2r1 = null;
		
		// Verify r2 head:false
		SolrDocumentList r2 = repositem.query(new SolrQuery("id:*@0000000002").setSort("path", ORDER.asc)).getResults();
		assertEquals("all rev items have head=false, " + r2.get(0), false, r2.get(0).get("head"));
		assertAllHeadFalse(r2);
		assertEquals("id has rev when head=false, " + r2.get(0), "t1.txt@0000000002", getTail(r2.get(0).get("id")));
		assertEquals("idhead never has rev, " + r2.get(0), "t1.txt", getTail(r2.get(0).get("idhead")));
		// url never had revision before repos-indexing 0.20
		assertEquals("url has rev when head=false, " + r2.get(0), "t1.txt?p=2", getTail(r2.get(0).get("url")));
		assertEquals("urlhead never has rev, " + r2.get(0), "t1.txt", getTail(r2.get(0).get("urlhead")));
		assertEquals("url has rev when head=false, " + r2.get(0), "t1.txt?p=2", getTail(r2.get(0).get("urlpath")));
		assertEquals("urlhead never has rev, " + r2.get(0), "t1.txt", getTail(r2.get(0).get("urlpathhead")));
		assertNull("urlid is null because the handler is not in repos-indexing core, " + r2.get(0), r2.get(0).get("urlid"));
		r2 = null;
		
		// Verify r2 head:true
		SolrDocumentList r2Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r2", 3, r2Head.size());
		assertEquals("folders are also head:true now", "/dir", r2Head.get(0).get("path"));
		assertEquals("...", "/dir/t2.txt", r2Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r2Head.get(2).get("path"));
		assertEquals("...", 1L, r2Head.get(0).get("rev"));
		assertEquals("...", 1L, r2Head.get(1).get("rev"));
		assertEquals("...", 2L, r2Head.get(2).get("rev"));
		assertEquals("...", 1L, r2Head.get(0).get("revc"));
		assertEquals("...", 1L, r2Head.get(1).get("revc"));
		assertEquals("...", 2L, r2Head.get(2).get("revc"));
		assertEquals("id has no rev when head=true, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("id")));
		assertEquals("idhead never has rev, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("idhead")));
		assertEquals("id has no rev when head=true, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("url")));
		assertEquals("idhead never has rev, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("urlhead")));
		assertEquals("id has no rev when head=true, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("urlpath")));
		assertEquals("idhead never has rev, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("urlpathhead")));
		assertNull("urlid is null because the handler is not in repos-indexing core, " + r2Head.get(2), r2Head.get(2).get("urlid"));
		r2Head = null;
		
				
		// Verify r1 after r3 head:false
		// everything from r1 should now have been replaced with later versions
		indexing.sync(new RepoRevision(3, new Date(3)));
		SolrDocumentList r3r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();		
		assertEquals("/dir", r3r1.get(0).get("path"));
		assertEquals("/dir/t2.txt", r3r1.get(1).get("path"));
		assertEquals("A", r3r1.get(0).get("pathstat"));
		assertEquals("A", r3r1.get(1).get("pathstat")); // Passes despite JSON response contains "java.lang.Character:A"
		assertEquals("/t1.txt", r3r1.get(2).get("path"));
		assertEquals("Should have revauthor.", "solsson", r3r1.get(2).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "Two files with two lines each", r3r1.get(2).getFieldValue("revcomment"));
		assertNull("should be null, unable to reproduce", r3r1.get(2).getFieldValue("prop_svn.entry.uuid"));
		assertEquals("Revision 1 had only these files, nothing else should have been indexed on rev 1 since then", 3, r3r1.size());

		// TODO support folders assertEquals("Folder is deleted and thus no longer in head", false, r3r1.get(0).get("head"));
		assertEquals("Old file that is now gone because of folder delete should not be head", false, r3r1.get(1).get("head"));
		assertEquals("The file that was changed in r3 should now be marked as non-head", false, r3r1.get(2).get("head"));
		r3r1 = null;
		
		// Verify r2 after r3 head:false
		SolrDocumentList r3r2 = repositem.query(new SolrQuery("id:*@0000000002").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was only a file edit in rev 2", 1, r3r2.size());
		assertEquals("/t1.txt", r3r2.get(0).get("path"));
		assertEquals("Should have revauthor.", "test", r3r2.get(0).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "file modification", r3r2.get(0).getFieldValue("revcomment"));
		assertEquals("all rev items have head=false", false, r3r2.get(0).get("head"));
		//assertEquals("Rev 2 is still HEAD for this file", true, r3r2.get(0).get("head"));
		r3r2 = null;
		
		// Verify r3 head:false
		SolrDocumentList r3r3 = repositem.query(new SolrQuery("id:*@0000000003").setSort("path", ORDER.asc)).getResults();
		assertEquals("Moved folder in rev 3, 2*2 changes with derived file.", 4, r3r3.size());
		assertEquals("Deletions should be indexed so we know when an item disappeared", "/dir", r3r3.get(0).get("path"));
		assertEquals("Derived delete", "/dir/t2.txt", r3r3.get(1).get("path"));
		assertEquals("Folder copy", "/dir2", r3r3.get(2).get("path"));
		assertEquals("Derived", "/dir2/t2.txt", r3r3.get(3).get("path"));
		
		assertEquals("Should have revauthor.", "test", r3r3.get(0).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "folder move without changes to the contained file", r3r3.get(0).getFieldValue("revcomment"));
		assertEquals("Should have revcauthor when not 'derived' from folder copy", "test", r3r3.get(0).getFieldValue("revcauthor"));
		assertEquals("Should have revccomment when not 'derived' from folder copy", "folder move without changes to the contained file", r3r3.get(0).getFieldValue("revccomment"));		
		assertEquals("Deletions should always be !head", false, r3r3.get(0).get("head"));
		assertEquals("Deletions should always be !head", false, r3r3.get(1).get("head"));
		assertEquals(false, r3r3.get(1).get("head"));
		assertAllHeadFalse(r3r3);
		r3r3 = null;

		// Verify r3 head:true
		SolrDocumentList r3Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r3", 3, r3Head.size());
		assertEquals("folders are also head:true now", "/dir2", r3Head.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r3Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r3Head.get(2).get("path"));
		assertEquals("...", 3L, r3Head.get(0).get("rev"));
		assertEquals("...", 3L, r3Head.get(1).get("rev"));
		assertEquals("...", 2L, r3Head.get(2).get("rev"));
		assertEquals("...", 3L, r3Head.get(0).get("revc"));
		assertEquals("...", 3L, r3Head.get(1).get("revc"));
		assertEquals("...", 2L, r3Head.get(2).get("revc"));
		r3Head = null;
		
		// Verify r4 commit info, where r4 does not actually exist (?).
		SolrDocumentList r4 = repositem.query(new SolrQuery("type:commit")).getResults();
		assertEquals(0L, r4.get(0).getFieldValue("rev"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.log"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.author"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.date"));
		
		assertEquals(1L, r4.get(1).getFieldValue("rev"));
		assertEquals("Two files with two lines each", r4.get(1).getFieldValue("proprev_svn.log"));
		assertEquals("solsson", r4.get(1).getFieldValue("proprev_svn.author"));
		assertEquals("2012-09-27T12:05:34.040515Z", r4.get(1).getFieldValue("proprev_svn.date"));
		
		assertEquals(2L, r4.get(2).getFieldValue("rev"));
		assertEquals("file modification", r4.get(2).getFieldValue("proprev_svn.log"));
		assertEquals("test", r4.get(2).getFieldValue("proprev_svn.author"));
		assertEquals("2013-03-21T19:16:28.271167Z", r4.get(2).getFieldValue("proprev_svn.date"));
		
		assertEquals(3L, r4.get(3).getFieldValue("rev"));
		assertEquals("folder move without changes to the contained file", r4.get(3).getFieldValue("proprev_svn.log"));
		assertEquals("test", r4.get(3).getFieldValue("proprev_svn.author"));
		assertEquals("2013-03-21T19:16:42.295071Z", r4.get(3).getFieldValue("proprev_svn.date"));
		r4 = null;
		
		// TODO we could propedit on dir2 and check that rev 3 of it becomes !head
		
		// TODO if we now modify t2 then latest dir2 should still be head
		
		// TODO if we then delete dir2 in the next commit we can demonstrate the issue with marking folders as !head when files have changed in them; need for workaround
	}	

	
	// Demonstrate path revision of folders.
	// TODO: Should path revision behave same as Subversion?
	@Test
	public void testMarkItemHeadFolderFileModified() throws IOException, SolrServerException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r4-filemodified.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		indexing.sync(new RepoRevision(4, new Date(4)));
	
		// Verify r4 head:true
		SolrDocumentList r4Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r4", 3, r4Head.size());
		assertEquals("folders are also head:true now", "/dir2", r4Head.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r4Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r4Head.get(2).get("path"));
		assertEquals("TODO? path rev should be r4 due to modified file", 3L, r4Head.get(0).get("rev"));
		assertEquals("file modified in r4", 4L, r4Head.get(1).get("rev"));
		assertEquals("...", 2L, r4Head.get(2).get("rev"));
		assertEquals("commit rev should be r3", 3L, r4Head.get(0).get("revc"));
		assertEquals("file modified in r4", 4L, r4Head.get(1).get("revc"));
		assertEquals("...", 2L, r4Head.get(2).get("revc"));
		r4Head = null;
	}
	
	
	@Test
	public void testMarkItemHeadCopy() throws IOException, SolrServerException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r5-copy.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		// Confirmed that each revision is indexed with r5 as reference revision.
		indexing.sync(new RepoRevision(5, new Date(5)));
		
		// Test that we can reindex without failure.
		
		SolrDocumentList r5r4 = repositem.query(new SolrQuery("id:*@0000000004").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was a folder and a file rev 4", 2, r5r4.size());
		
		assertEquals("...", "/dir2-copy", r5r4.get(0).get("path"));
		assertEquals("...", "/dir2-copy/t2.txt", r5r4.get(1).get("path"));
		
		assertEquals("...", 4L, r5r4.get(0).get("rev"));
		assertEquals("...", 4L, r5r4.get(1).get("rev"));
		
		assertEquals("...", 4L, r5r4.get(0).get("revc"));
		assertEquals("...", 4L, r5r4.get(1).get("revc"));
		assertAllHeadFalse(r5r4);
		r5r4 = null;
		
		SolrDocumentList r5 = repositem.query(new SolrQuery("id:*@0000000005").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was a file r5", 1, r5.size());
		assertEquals("...", "/dir2/t2.txt", r5.get(0).get("path"));
		assertEquals("...", "D", r5.get(0).get("pathstat"));
		assertAllHeadFalse(r5);
		
		// Verify r5 head:true
		SolrDocumentList r5Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r5", 4, r5Head.size());
		assertEquals("...", "/dir2", r5Head.get(0).get("path"));
		assertEquals("...", "/dir2-copy", r5Head.get(1).get("path"));
		assertEquals("...", "/dir2-copy/t2.txt", r5Head.get(2).get("path"));
		assertEquals("...", "/t1.txt", r5Head.get(3).get("path"));
		assertEquals("TODO? folder not indexed when containing file deleted", 3L, r5Head.get(0).get("rev"));
		assertEquals("...", 4L, r5Head.get(1).get("rev"));
		assertEquals("...", 4L, r5Head.get(2).get("rev"));
		assertEquals("...", 2L, r5Head.get(3).get("rev"));
		assertEquals("...", 3L, r5Head.get(0).get("revc"));
		assertEquals("...", 4L, r5Head.get(1).get("revc"));
		assertEquals("...", 4L, r5Head.get(2).get("revc"));
		assertEquals("...", 2L, r5Head.get(3).get("revc"));
		r5Head = null;
	}
	
	@Test
	public void testMarkItemHeadCopyDeleted() throws SolrServerException, IOException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r6-copydeleted.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		// Confirmed that each revision is indexed with r5 as reference revision.
		indexing.sync(new RepoRevision(6, new Date(6)));
				
		// Test that we can reindex without failure.
		System.out.println("Test that we can reindex without failure.");
		
		SolrDocumentList r6 = repositem.query(new SolrQuery("id:*@0000000006").setSort("path", ORDER.asc)).getResults();
		assertEquals("Restored a folder and two files in rev 6", 3, r6.size());
		assertEquals("...", "/dir2", r6.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r6.get(1).get("path"));
		assertEquals("...", "/dir2/t3.txt", r6.get(2).get("path"));
		assertAllHeadFalse(r6);
		r6 = null;
		
		// Verify r6 head:true
		SolrDocumentList r6Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r6", 4, r6Head.size());
		assertEquals("...", "/dir2", r6Head.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r6Head.get(1).get("path"));
		assertEquals("...", "/dir2/t3.txt", r6Head.get(2).get("path"));
		assertEquals("...", "/t1.txt", r6Head.get(3).get("path"));
		assertEquals("folder restored", 6L, r6Head.get(0).get("rev"));
		assertEquals("...", 6L, r6Head.get(1).get("rev"));
		assertEquals("...", 6L, r6Head.get(2).get("rev"));
		assertEquals("...", 2L, r6Head.get(3).get("rev"));
		assertEquals("...", 6L, r6Head.get(0).get("revc"));
		assertEquals("...", 6L, r6Head.get(1).get("revc"));
		assertEquals("...", 6L, r6Head.get(2).get("revc"));
		assertEquals("...", 2L, r6Head.get(3).get("revc"));
		r6Head = null;
	}
	
	@Test
	public void testMarkItemHeadAddDeleted() throws SolrServerException, IOException {
		String dumpFileName = "se/repos/indexing/testrepo1r7-adddeleted.svndump";
		logger.info("Testing: {}", dumpFileName);
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(dumpFileName);
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		// Indexing whole repo in batch does not reproduce the issue. 
		// Batch index first era of t1.txt.
		indexing.sync(new RepoRevision(5, new Date(5)));
		// Incrementally index remaining revisions.
		indexing.sync(new RepoRevision(6, new Date(6)));
		indexing.sync(new RepoRevision(7, new Date(7)));
		
		/*
		 * r4: Moves t1.txt to t1-renamed.txt
		 * r5: Deletes t1-renamed.txt
		 * r6: Adds a new t1.txt
		 * r7: Deletes the new t1.txt
		 */
		
		SolrDocumentList r7r7 = repositem.query(new SolrQuery("id:*@0000000007").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("Only a single file removed in rev 7", 1, r7r7.size());
		logger.info("Done: {}", dumpFileName);
	}
	
	@Test
	public void testMarkItemHeadAddDeletedSync4() throws SolrServerException, IOException {
		String dumpFileName = "se/repos/indexing/testrepo1r7-adddeleted.svndump";
		logger.info("Testing: {}", dumpFileName);
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(dumpFileName);
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		// Indexing whole repo in batch does not reproduce the issue. 
		// Batch index first era of t1.txt.
		indexing.sync(new RepoRevision(4, new Date(4)));
		indexing.sync(new RepoRevision(5, new Date(5)));
		// Incrementally index remaining revisions.
		indexing.sync(new RepoRevision(6, new Date(6)));
		indexing.sync(new RepoRevision(7, new Date(7)));
		
		/*
		 * r4: Moves t1.txt to t1-renamed.txt
		 * r5: Deletes t1-renamed.txt
		 * r6: Adds a new t1.txt
		 * r7: Deletes the new t1.txt
		 */
		
		SolrDocumentList r7r7 = repositem.query(new SolrQuery("id:*@0000000007").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("Only a single file removed in rev 7", 1, r7r7.size());
		logger.info("Done: {}", dumpFileName);
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
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
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
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
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
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		
		assertEquals(3, repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().size());
		
		ReposIndexing indexing2 = context.getInstance(ReposIndexing.class);
		assertTrue("This test is uninteresting if context has a singleton", indexing2 != indexing);
		indexing2.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		repositem.commit(); // to be sure that second sync doesn't do any solr operations
		
		assertEquals(3, repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().size());
	}	
	
	@Test
	public void testIndexingModeNone() throws SolrServerException, IOException {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1r3-indexing-mode-none.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		indexing.sync(RepoRevision.parse("3/2013-03-21T19:16:42.295Z"));
		assertEquals("should have indexed up to the given revision", 3, indexing.getRevision().getNumber());
		
		SolrClient repositem = context.getInstance(Key.get(SolrClient.class, Names.named("repositem")));
		assertEquals("total head items is 3, but one is suppressed in r2", 3-1, repositem.query(new SolrQuery("head:true")).getResults().size());
		assertEquals("should have indexed rev 1 (without commit item)", 3, repositem.query(new SolrQuery("id:*@0000000001")).getResults().size());
		assertEquals("should have indexed rev 1, one item remains head but suppressed in r2", 1+3, repositem.query(new SolrQuery("rev:1")).getResults().size());
		assertEquals("should only index the commit for rev 2", 1, repositem.query(new SolrQuery("rev:2")).getResults().size());
		assertEquals("should have indexed rev 3 (commit item)", 1, repositem.query(new SolrQuery("id:*#0000000003")).getResults().size());
		assertEquals("should have indexed rev 3 (rev items)", 4, repositem.query(new SolrQuery("id:*@0000000003")).getResults().size());
		assertEquals("should have indexed rev 3 (head item)", 2, repositem.query(new SolrQuery("rev:3 AND head:true")).getResults().size());
		assertEquals("should have indexed rev 3", 1+4+2, repositem.query(new SolrQuery("rev:3")).getResults().size());
		
	}
	
	private void assertAllHeadFalse(SolrDocumentList docs) {
		for (int i = 0; i < docs.size(); i++) {
			assertEquals("at " + docs.get(i).get("path"), false, docs.get(i).get("head"));
		}
	}
	
}
