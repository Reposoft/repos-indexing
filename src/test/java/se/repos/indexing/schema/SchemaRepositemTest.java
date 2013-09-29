/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.schema;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

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
		
		assertEquals("Making property search case insentitive wouldn't be good when props contain URLs etc",
				0, solr.query(new SolrQuery("prop_custom.tags:junit")).getResults().getNumFound());
	}

}
