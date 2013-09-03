/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.testconfig;

import org.apache.solr.client.solrj.SolrServer;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;

import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.ItemContentsBufferStrategy;
import se.repos.indexing.item.ItemPathinfo;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.twophases.ItemContentsNocache;
import se.repos.indexing.twophases.ItemPropertiesImmediate;
import se.repos.indexing.twophases.ReposIndexingImpl;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsRepositoryLookupSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.SvnlookClientProviderStateless;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Indexing configuration for our tests in this module.
 * See also the distributed standard config for unit tests, {@link TestIndexOptions#getIndexing()}.
 */
public class IndexingTestModule extends AbstractModule {

	private SolrServer repositem;

	public IndexingTestModule(SolrServer solrRepositemCore) {
		this.repositem = solrRepositemCore;
	}
	
	@Override
	protected void configure() {
		bind(SolrServer.class).annotatedWith(Names.named("repositem"))
			.toInstance(repositem);
		
		bind(ReposIndexing.class).to(ReposIndexingImpl.class);
		
		Multibinder<IndexingItemHandler> blocking = Multibinder.newSetBinder(binder(), IndexingItemHandler.class, Names.named("blocking"));
		@SuppressWarnings("unused") // background is of little use in tests
		Multibinder<IndexingItemHandler> background = Multibinder.newSetBinder(binder(), IndexingItemHandler.class, Names.named("background"));
		
		blocking.addBinding().to(ItemPathinfo.class);
		
		bind(ItemContentsBufferStrategy.class).to(ItemContentsNocache.class);
		bind(ItemPropertiesBufferStrategy.class).to(ItemPropertiesImmediate.class);
		
		// backend-svnkit
		bind(SVNLookClient.class).toProvider(SvnlookClientProviderStateless.class);
		bind(CmsChangesetReader.class).to(CmsChangesetReaderSvnkitLook.class);
		bind(CmsContentsReader.class).to(CmsContentsReaderSvnkitLook.class);
		bind(CmsRepositoryLookup.class).annotatedWith(Names.named("inspection")).to(CmsRepositoryLookupSvnkitLook.class);
		
		// tweaks
		bind(ItemContentsBufferStrategy.class).to(ItemContentsNocache.class);
	}

}
