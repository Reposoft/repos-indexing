/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

/**
 * Default toString for HttpSolrServer is unhelpul in logs.
 * We often deal with multiple cores and share some code between them, so it is useful to log core name/identifier.
 */
public class HttpSolrServerNamed extends HttpSolrServer {

	public HttpSolrServerNamed(String baseURL) {
		super(baseURL);
	}

	public HttpSolrServerNamed(String baseURL, HttpClient client) {
		super(baseURL, client);
	}
	
	public HttpSolrServerNamed(String arg0, HttpClient arg1, ResponseParser arg2) {
		super(arg0, arg1, arg2);
	}

	private static final long serialVersionUID = 1L;

	private String identifier ;

	public HttpSolrServerNamed setName(String identifier) {
		this.identifier = identifier;
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSuperclass().getName() + "(" + identifier + ")";
	}
	
}
