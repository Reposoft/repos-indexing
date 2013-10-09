/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.testconfig;

import org.apache.solr.client.solrj.SolrServer;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IdStrategyDefault;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.twophases.IndexingEventAware;
import se.repos.indexing.twophases.ItemContentsMemoryChoiceDeferred;
import se.repos.indexing.twophases.ItemPropertiesImmediate;
import se.repos.indexing.twophases.ReposIndexingImpl;
import se.repos.indexing.twophases.RepositoryIndexStatus;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsRepositoryLookupSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CommitRevisionCache;
import se.simonsoft.cms.backend.svnkit.svnlook.CommitRevisionCacheDefault;
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
		
		blocking.addBinding().to(HandlerPathinfo.class);
		blocking.addBinding().to(HandlerProperties.class);
		
		@SuppressWarnings("unused") // not necessarily needed, because other dependencies are picked up as aware too
		Multibinder<IndexingEventAware> additionalEventHandlers = Multibinder.newSetBinder(binder(), IndexingEventAware.class);
		
		// backend-svnkit
		bind(SVNLookClient.class).toProvider(SvnlookClientProviderStateless.class);
		bind(CmsChangesetReader.class).to(CmsChangesetReaderSvnkitLook.class);
		bind(CommitRevisionCache.class).to(CommitRevisionCacheDefault.class);
		bind(CmsContentsReader.class).to(CmsContentsReaderSvnkitLook.class);
		bind(CmsRepositoryLookup.class).annotatedWith(Names.named("inspection")).to(CmsRepositoryLookupSvnkitLook.class);
		
		// configure indexing
		bind(IdStrategy.class).to(IdStrategyDefault.class);
		bind(RepositoryIndexStatus.class);
		bind(ItemPropertiesBufferStrategy.class).to(ItemPropertiesImmediate.class);
		bind(ItemContentBufferStrategy.class).to(ItemContentsMemoryChoiceDeferred.class);
		bind(Integer.class).annotatedWith(Names.named("indexingFilesizeInMemoryLimitBytes")).toInstance(100000); // optimize for test run performance, but we should test the file cache also
	}

}
