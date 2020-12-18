/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class HandlerHeadinfoTest {
	
	@Captor
    private ArgumentCaptor<Collection<SolrInputDocument>> addCaptor;
	
	@Before
    public void init(){
        MockitoAnnotations.initMocks(this);
    }

	@Test
	public void testHandle() throws SolrServerException, IOException {
		CmsRepository repository = new CmsRepository("http://testhost/svn/repo");
		RepoRevision rev1 = new RepoRevision(1, new Date(1));
		RepoRevision rev2 = new RepoRevision(2, new Date(2));
		
		IndexingDoc a1doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem a1i = mock(CmsChangesetItem.class);
		when(a1i.getPath()).thenReturn(new CmsItemPath("/a.txt"));
		IndexingItemProgress a1p = mock(IndexingItemProgress.class);
		when(a1p.getRepository()).thenReturn(repository);
		when(a1p.getRevision()).thenReturn(rev1);
		when(a1p.getFields()).thenReturn(a1doc);
		when(a1p.getItem()).thenReturn(a1i);
		
		IndexingDoc a2doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem a2i = mock(CmsChangesetItem.class);
		when(a2i.getPath()).thenReturn(new CmsItemPath("/a.txt"));		
		IndexingItemProgress a2p = mock(IndexingItemProgress.class);
		when(a2p.getRepository()).thenReturn(repository);
		when(a2p.getRevision()).thenReturn(rev2);		
		when(a2p.getFields()).thenReturn(a2doc);
		when(a2p.getItem()).thenReturn(a2i);
		
		IndexingDoc b1doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem b1i = mock(CmsChangesetItem.class);
		when(b1i.getPath()).thenReturn(new CmsItemPath("/b.txt"));		
		IndexingItemProgress b1p = mock(IndexingItemProgress.class);
		when(b1p.getRepository()).thenReturn(repository);
		when(b1p.getRevision()).thenReturn(rev1);		
		when(b1p.getFields()).thenReturn(b1doc);
		when(b1p.getItem()).thenReturn(b1i);
		
		IndexingDoc b2doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem b2i = mock(CmsChangesetItem.class);
		when(b2i.getPath()).thenReturn(new CmsItemPath("/b.txt"));		
		IndexingItemProgress b2p = mock(IndexingItemProgress.class);
		when(b2p.getRepository()).thenReturn(repository);
		when(b2p.getRevision()).thenReturn(rev2);		
		when(b2p.getFields()).thenReturn(b2doc);
		when(b2p.getItem()).thenReturn(b2i);
		
		SolrClient repositem = mock(SolrClient.class);
		
		HandlerHeadinfo headinfo = new HandlerHeadinfo(null);
		headinfo.setSolrClient(repositem);
		headinfo.setIdStrategy(new IdStrategyDefault());
		// first sync
		when(a1i.isOverwritten()).thenReturn(true);
		when(a1i.isAdd()).thenReturn(true);
		when(b1i.isAdd()).thenReturn(true);
		when(a2i.getRevisionObsoleted()).thenReturn(rev1);
		headinfo.handle(a1p);
		headinfo.handle(a2p);
		headinfo.handle(b1p);
		assertEquals("should set head flag", true, b1doc.getFieldValue("head"));
		assertEquals("a1 is marked as being overwritten", false, a1doc.getFieldValue("head"));
		assertEquals(true, a2doc.getFieldValue("head"));
		
		// second sync
		when(b2i.getRevisionObsoleted()).thenReturn(rev1);
		headinfo.handle(b2p);
		
		verify(repositem).add(addCaptor.capture());
		verifyNoMoreInteractions(repositem);
		assertEquals("Should only have done one head status update, because internal state shold say that a1 was already marked as non-head at 1", 1, addCaptor.getAllValues().size());
	}

	@Test
	public void testHandleDelete() throws SolrServerException, IOException {
		CmsRepository repository = new CmsRepository("http://testhost/svn/repo");
		RepoRevision rev1 = new RepoRevision(1, new Date(1));
		RepoRevision rev2 = new RepoRevision(2, new Date(2));
		
		IndexingDoc a1doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem a1i = mock(CmsChangesetItem.class);
		when(a1i.getPath()).thenReturn(new CmsItemPath("/a.txt"));
		IndexingItemProgress a1p = mock(IndexingItemProgress.class);
		when(a1p.getRepository()).thenReturn(repository);
		when(a1p.getRevision()).thenReturn(rev1);
		when(a1p.getFields()).thenReturn(a1doc);
		when(a1p.getItem()).thenReturn(a1i);
		
		IndexingDoc a2doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem a2i = mock(CmsChangesetItem.class);
		when(a2i.getPath()).thenReturn(new CmsItemPath("/a.txt"));		
		IndexingItemProgress a2p = mock(IndexingItemProgress.class);
		when(a2p.getRepository()).thenReturn(repository);
		when(a2p.getRevision()).thenReturn(rev2);		
		when(a2p.getFields()).thenReturn(a2doc);
		when(a2p.getItem()).thenReturn(a2i);
		
		when(a1i.isAdd()).thenReturn(true);
		when(a2i.isDelete()).thenReturn(true);
		when(a2i.getRevisionObsoleted()).thenReturn(rev1);
		
		SolrClient repositem = mock(SolrClient.class);
		HandlerHeadinfo headinfo = new HandlerHeadinfo(null);
		headinfo.setSolrClient(repositem);
		IdStrategyDefault idStrategy = new IdStrategyDefault();
		headinfo.setIdStrategy(idStrategy);
		
		headinfo.handle(a1p);
		assertEquals(true, a1p.getFields().getFieldValue("head"));
		headinfo.handle(a2p);
		assertEquals("Index should contain delete entries, set to head=false", false, a2p.getFields().getFieldValue("head"));
		
		//ArgumentCaptor<SolrInputDocument> addCaptor = ArgumentCaptor.forClass(Collection<SolrInputDocument.class>.class);
		verify(repositem).add(addCaptor.capture());
		assertEquals("Should have set head=false for the deleted", 1, addCaptor.getAllValues().size());
		SolrInputDocument deleted = addCaptor.getValue().iterator().next();
		assertEquals(idStrategy.getId(repository, rev1, a1i.getPath()), deleted.getFieldValue("id"));
		assertEquals("Should be a field update request, not overwriting values", "{set=false}", deleted.getFieldValue("head").toString());
		assertEquals("Shouldn't do anything else with the deleted", 2, deleted.getFieldNames().size());
	}
	
}
