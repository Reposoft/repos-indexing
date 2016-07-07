/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.schema;

import java.io.IOException;
import java.util.Date;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.*;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class SchemaRepositemTest extends SolrTestCaseJ4 {

	public static String solrhome = "se/repos/indexing/solr";
	
	private SolrServer solrTestServer = null;

	@BeforeClass
	public static void beforeTests() throws Exception {
		try {
			SolrTestCaseJ4.initCore(solrhome + "/repositem/conf/solrconfig.xml", solrhome + "/repositem/conf/schema.xml",
					"src/test/resources/" + solrhome); // has to be in classpath because "collection1" is hardcoded in TestHarness initCore/createCore
		} catch (Exception e) {
			System.out.println("getSolrConfigFile()=" + getSolrConfigFile());
			System.out.println("testSolrHome=" + testSolrHome);
			throw e;
		}
	}
	
	@After
	public void tearDown() throws Exception {
		//printHits(new SolrQuery("*:*"));
		// tests have different repositories so let's see if they can use the same solr instance //solrTestServer = null;
		// clear data from this test
		//getSolr().deleteByQuery("*:*");
		super.tearDown();
	}
	
	@SuppressWarnings("unused")
	private void printHits(SolrQuery q) throws SolrServerException {
		System.out.println("--- solr contents " + q.toString() + " ---");
		SolrDocumentList results = getSolr().query(q).getResults();
		if (results.size() == 0) {
			System.out.println("empty");
		}
		for (SolrDocument d : results) {
			for (String f : d.getFieldNames()) {
				String v = "" + d.get(f);
				System.out.print(", " + f + ": " + v);
			}
			System.out.println("");
		}
	}

	/**
	 * @return instance for injection when integration testing our logic with solr, for index testing we do fine with SolrTestCaseJ4 helper methods
	 */
	public SolrServer getSolr() {
		if (solrTestServer == null) {
			solrTestServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
		}
		return solrTestServer;
	}
	
	@Test
	public void testPropertySearch() throws Exception {
		SolrServer solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("prop_svn.ignore", "ignore1\nignore2");
		doc.addField("prop_svn.externals", "ext1 ^/some/folder\next2\t^/some/other/folder");
		doc.addField("prop_custom.lang", "se-SE | de-SE | en-US");
		doc.addField("prop_custom.values", "whatever, Value,Wanted");
		doc.addField("prop_custom.values2", "Semi;colon ;Separated");
		doc.addField("prop_custom.tags", "testing validation JUnit");
		doc.addField("prop_custom.json", "[{\"jsonkey\":\"jsonval\"}, \"justval\"]");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should tokenize on newline", 1, solr.query(new SolrQuery("prop_svn.ignore:ignore2")).getResults().getNumFound());
		assertEquals("Should still match the full value", 1, solr.query(new SolrQuery("prop_svn.ignore:ignore1\nignore2")).getResults().getNumFound());
		//assertEquals("What id the full value has one token that doesn't match?", 0, solr.query(new SolrQuery("prop_svn.ignore:ignore1\nignore0")).getResults().getNumFound());
		assertEquals("Should tokenize on whitespace", 1, solr.query(new SolrQuery("prop_svn.externals:\"^/some/folder\"")).getResults().getNumFound());
		assertEquals("Should tokenize on tab", 1, solr.query(new SolrQuery("prop_svn.externals:\"^/some/other/folder\"")).getResults().getNumFound());
		assertEquals("Should match on line", 1, solr.query(new SolrQuery("prop_svn.externals:\"ext1 ^/some/folder\"")).getResults().getNumFound());
		assertEquals("What if a line has a mismatching token", 0, solr.query(new SolrQuery("prop_svn.externals:\"ext0 ^/some/folder\"")).getResults().getNumFound());
		//assertEquals("What if a line has a mismatching token that exists somewhere else", 0, solr.query(new SolrQuery("prop_svn.externals:\"ext2 ^/some/folder\"")).getResults().getNumFound());
		assertEquals("Could match on line regarless of whitespace", 1, solr.query(new SolrQuery("prop_svn.externals:\"ext2 ^/some/other/folder\"")).getResults().getNumFound());
		assertEquals("Should separate on comma", 1, solr.query(new SolrQuery("prop_custom.values:Value")).getResults().getNumFound());
		assertEquals("Should separate on semicolon", 1, solr.query(new SolrQuery("prop_custom.values2:Separated")).getResults().getNumFound());
		//how?//assertEquals("Should separate on pipe", 1, solr.query(new SolrQuery("prop_custom.lang:de-DE")).getResults().getNumFound());
		assertEquals("Should separate on whitespace", 1, solr.query(new SolrQuery("prop_custom.tags:JUnit")).getResults().getNumFound());
		
		/*
		assertEquals("Making property search case insensitive wouldn't be good when props contain URLs etc",
				0, solr.query(new SolrQuery("prop_custom.tags:junit")).getResults().getNumFound());
		*/
		assertEquals("Expecting property search to be case insensitive.",
				1, solr.query(new SolrQuery("prop_custom.tags:junit")).getResults().getNumFound());
	}
	
	@Test
	public void testFulltextSearchCamelCase() throws Exception {
		SolrServer solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word JavaClassName getMethodName The ProductNAME followed by text");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match words in sequence", 1, solr.query(new SolrQuery("text:followed by text")).getResults().getNumFound());
		assertEquals("Should match quoted words in sequence", 1, solr.query(new SolrQuery("text:\"followed by text\"")).getResults().getNumFound());
		assertEquals("Should not match quoted words out of sequence", 0, solr.query(new SolrQuery("text:\"followed text\"")).getResults().getNumFound());

		
		assertEquals("Should match Java Class Name camelcase", 1, solr.query(new SolrQuery("text:JavaClassName")).getResults().getNumFound());
		assertEquals("Should match Java Class Name lowercase", 1, solr.query(new SolrQuery("text:javaclassname")).getResults().getNumFound());
		assertEquals("Should match Java Method Name camelcase", 1, solr.query(new SolrQuery("text:getMethodName")).getResults().getNumFound());
		assertEquals("Should match Java Method Name lowercase", 1, solr.query(new SolrQuery("text:getmethodname")).getResults().getNumFound());
		assertEquals("Should match Java Method Name wildcard", 1, solr.query(new SolrQuery("text:getmethod*")).getResults().getNumFound());
		
		assertEquals("Should match Product Name case-switch", 1, solr.query(new SolrQuery("text:ProductNAME")).getResults().getNumFound());
		assertEquals("Should match Product Name lowercase", 1, solr.query(new SolrQuery("text:productname")).getResults().getNumFound());
		assertEquals("Should match Product Name leading capital", 1, solr.query(new SolrQuery("text:Productname")).getResults().getNumFound());
		assertEquals("Should match Product Name in context", 1, solr.query(new SolrQuery("text:The ProductNAME followed by text")).getResults().getNumFound());
		// Will fail if using preserveOriginal="1".
		assertEquals("Should match Product Name in context - Quoted", 1, solr.query(new SolrQuery("text:\"The ProductNAME followed by text\"")).getResults().getNumFound());
		assertEquals("Should match Product Name lowercase in context - Quoted", 1, solr.query(new SolrQuery("text:\"The Productname followed by text\"")).getResults().getNumFound());

		// Difficult to combine individual components with quoted search.
		/*
		assertEquals("Could match Product Name individual components", 1, solr.query(new SolrQuery("text:product")).getResults().getNumFound());
		assertEquals("Could match Product Name individual components", 1, solr.query(new SolrQuery("text:name")).getResults().getNumFound());
		assertEquals("Could match Product Name separated components", 1, solr.query(new SolrQuery("text:product name")).getResults().getNumFound());
		*/
	}

	@Test
	public void testFulltextSearchDelimiters() throws Exception {
		SolrServer solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word top-level");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match exact", 1, solr.query(new SolrQuery("text:top-level")).getResults().getNumFound());
		assertEquals("Should match part 1", 1, solr.query(new SolrQuery("text:top")).getResults().getNumFound());
		assertEquals("Should match part 2", 1, solr.query(new SolrQuery("text:level")).getResults().getNumFound());
		
		// Below assert just documents current behavior, would be fine if they also hit.
		assertEquals("Unlikely to match catenated", 0, solr.query(new SolrQuery("text:toplevel")).getResults().getNumFound());
	}
	
	
	// Documents the effect of the caveat in http://wiki.apache.org/solr/Atomic_Updates
	@Test
	public void testHeadFlagUpdateEffect() throws Exception {
		SolrServer solr = getSolr();
		
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		doc.addField("id", "f#01");
		doc.addField("head", true);
		doc.addField("pathstat", "A");
		doc.addField("path", "dir/file.txt");
		doc.addField("pathext", "txt");
		doc.addField("text", "quite secret content, though searchable");
		solr.add(doc.getSolrDoc());
		solr.commit();
		assertEquals("Should be searchable on path", 1, solr.query(new SolrQuery("path:dir*")).getResults().getNumFound());
		assertEquals("Should be searchable on pathext", 1, solr.query(new SolrQuery("pathext:txt")).getResults().getNumFound());
		assertEquals("Should be searchable on text", 1, solr.query(new SolrQuery("text:secret")).getResults().getNumFound());
		
		IndexingDocIncrementalSolrj docd = new IndexingDocIncrementalSolrj();
		docd.addField("id", "f#02");
		docd.addField("head", true);
		docd.addField("pathstat", "D");
		docd.addField("path", "dir/file.txt");
		docd.addField("pathext", "txt");		
		doc.setUpdateMode(true);
		doc.setField("head", false);
		solr.add(docd.getSolrDoc());
		solr.add(doc.getSolrDoc());
		solr.commit();
		assertEquals("Both head and historical should be searchable on path",
				2, solr.query(new SolrQuery("path:dir*")).getResults().getNumFound());
		assertEquals("Both head and historical should be searchable on pathext",
				2, solr.query(new SolrQuery("pathext:txt")).getResults().getNumFound());
		assertEquals("Text search for historical has been scoped out, if made stored it might affect access control requirements", 
		//assertEquals("Historical should still be searchable on text after head flag update",
				0, solr.query(new SolrQuery("text:secret")).getResults().getNumFound());
	}

	@Test
	@Ignore // very incomplete
	public void testPathAnalysis() throws SolrServerException, IOException {
		SolrServer solr = getSolr();		
		// Not really unit testing the schema here because the path logic in the handler is too relevant - could be switched to 
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsRepository repo = new CmsRepository("http://ex.ampl:444/s/rep1");
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(new RepoRevision(35L, new Date()));
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath())
			.thenReturn(new CmsItemPath("/dir/doc main.xml"))
			.thenReturn(new CmsItemPath("/dir sect/doc appendix.xml"));
			;
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getItem()).thenReturn(item);
		HandlerPathinfo handlerPathinfo = new HandlerPathinfo();
		handlerPathinfo.handle(p);
		solr.add(doc.getSolrDoc());
		
		fail("need to test effects of analyzed path* fields and confirm the need for name and extension");
	}
	
}
