package se.repos.indexing.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.RepositoryInfo;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.xml.sax.InputSource;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexingHandlers;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IdStrategyDefault;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.repos.indexing.scheduling.IndexingScheduleBlockingOnly;
import se.repos.indexing.twophases.ItemContentsMemory;
import se.repos.indexing.twophases.ItemContentsMemorySizeLimit;
import se.repos.indexing.twophases.ItemContentsNocache;
import se.repos.indexing.twophases.ItemPropertiesImmediate;
import se.repos.indexing.twophases.RepositoryIndexStatus;
import se.simonsoft.cms.backend.svnkit.CmsRepositorySvn;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLookRepo;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
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

public class ReposIndexingPerRepositoryIntegrationTest {

	private Injector context = null;
	
	@Before
	public void setUp() throws Exception {
		File dataDir = new File(System.getProperty("java.io.tmpdir") + "/solrdata-" + this.getClass().getName());
		if (dataDir.exists()) {
			FileUtils.deleteDirectory(dataDir);
		}
		
		SolrResourceLoader resourceLoader = new SolrResourceLoader(dataDir.getAbsolutePath());
		CoreContainer coreContainer = new CoreContainer(resourceLoader);
		//CoreDescriptor coreDescriptor = coreContainer.getCoreDescriptor("repositem");
		CoreDescriptor coreDescriptor = new CoreDescriptor(coreContainer, "repositem", dataDir.getAbsolutePath());
		
		//InputStream solrconfxml = getClass().getClassLoader().getResourceAsStream("se/repos/indexing/solr/repositem/conf/solrconfig.xml");
		//InputStream schemaxml = getClass().getClassLoader().getResourceAsStream("se/repos/indexing/solr/repositem/conf/schema.xml");
		// need actual files so it can be loaded in standalone solr
		File solrconfxml = new File("src/main/resources/se/repos/indexing/solr/repositem/conf/solrconfig.xml");
		assertTrue(solrconfxml.exists());
		File schemaxml = new File("src/main/resources/se/repos/indexing/solr/repositem/conf/schema.xml");
		assertTrue(schemaxml.exists());
		SolrConfig solrconfig = new SolrConfig("solrconfig.xml",  new InputSource(new FileInputStream(solrconfxml)));
		IndexSchema schema = new IndexSchema(solrconfig, "schema.xml", new InputSource(new FileInputStream(schemaxml)));
		
		SolrCore repositemCore = new SolrCore("repositem", dataDir.getAbsolutePath(), solrconfig, schema, coreDescriptor);
		coreContainer.load();
		
		final SolrServer repositem = new EmbeddedSolrServer(coreContainer, "repositem");

		System.out.println("Load core using:");
		System.out.println("http://localhost:8983/solr/admin/cores?action=CREATE&name=repositem&config=" + solrconfxml.getAbsolutePath() + "&schema=" + schemaxml.getAbsolutePath() + "&dataDir=" + dataDir.getAbsolutePath());
		
		final CmsTestRepository repository = SvnTestSetup.getInstance().getRepository();
		
		Module backend = new AbstractModule() { @Override protected void configure() {
			bind(CmsRepository.class).toInstance(repository);
			bind(CmsRepositorySvn.class).toInstance(new CmsRepositorySvn(repository.getUrl(), repository.getAdminPath()));
			bind(CmsTestRepository.class).toInstance(repository); // backend specific type, should be called CmsSvnTestRepository
			
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
	
	@After
	public void tearDown() throws Exception {
		SvnTestSetup.getInstance().tearDown();
	}
	
	@Test
	public void test() throws SolrServerException, IOException {
		
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/repos/indexing/testrepo1.svndump");
		assertNotNull(dumpfile);
		context.getInstance(CmsTestRepository.class).load(dumpfile);
		
		ReposIndexing indexing = context.getInstance(ReposIndexing.class);
		context.getInstance(IndexingSchedule.class).start();
		
		// these two should possibly be moved to RepositoryIndexStatus
		assertEquals(null, indexing.getRevComplete(null));
		assertEquals(null, indexing.getRevProgress(null));
		
		indexing.sync(null, new RepoRevision(1, new Date(1))); // 2012-09-27T12:05:34.040515Z
		assertNotNull("Should track indexing", indexing.getRevComplete(null));
		assertEquals("should have indexed up to the given revision", 1, indexing.getRevComplete(null).getNumber());
		
		SolrServer repositem = context.getInstance(Key.get(SolrServer.class, Names.named("repositem")));
		
		QueryResponse r1 = repositem.query(new SolrQuery("type:commit").addSort("rev", ORDER.asc));
		assertEquals("Rev 0 should have been indexed in addition to 1", 2, r1.getResults().size());
		assertEquals("Rev 0 should be marked as completed", true, r1.getResults().get(0).getFieldValue("complete"));
		
		// new indexing service, recover sync status
		ReposIndexing indexing2 = context.getInstance(ReposIndexing.class);
		indexing2.sync(null, new RepoRevision(1, new Date(1))); // polling now done at sync
		assertNotNull("New indexing should poll for indexed revision",
				indexing2.getRevComplete(null));
		assertTrue("New indexing should poll for highest indexed revision", 
				indexing2.getRevComplete(null).getNumber() == 1);
	
		// mess with the index to see how sync status is handled
		SolrInputDocument fake2 = new SolrInputDocument();
		String id2 = r1.getResults().get(1).getFieldValue("id").toString().replace("#1", "#2");
		fake2.setField("id", id2);
		fake2.setField("complete", true);
		repositem.add(fake2);
		repositem.commit();
		assertEquals("Service is not expected to handle cuncurrent indexing", 1, indexing2.getRevComplete(null).getNumber());
		
		ReposIndexing indexing3 = context.getInstance(ReposIndexing.class);
		indexing3.sync(null, new RepoRevision(1, new Date(1))); // polling now done at sync
		assertEquals("New indexing service should not mistake aborted indexing as completed", 1, indexing3.getRevComplete(null).getNumber());
		//not implemented//assertEquals("New indexing service should see that a revision has started but not completed", 2, indexing3.getRevProgress(repo).getNumber());
		
		try {
			indexing3.sync(null, new RepoRevision(2, new Date(2)));
			fail("Should attempt to index rev 2 because it is marked as in progress and the new indexing instance does not know the state of that operation so it has to assume that it was aborted");
		} catch (Exception e) {
			// expected, there is no revision 2
		}
	}

}
