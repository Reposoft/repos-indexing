/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class ItemPathinfoTest {

	@Test
	public void testHandle() {
		ItemPathinfo pathinfo = new ItemPathinfo();
		CmsRepository repo = new CmsRepository("https://h.ost:1080/svn/repo1");
		RepoRevision rev = new RepoRevision(10L, new Date());
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingItemProgress p = new IndexingItemStandalone(repo, rev, item);
		
		when(item.getPath()).thenReturn(new CmsItemPath("/my/dir/file.txt"));
		when(item.isFile()).thenReturn(true);
		when(item.isFolder()).thenReturn(false);
		when(item.getRevision()).thenReturn(new RepoRevision(rev.getNumber() - 2, new Date(rev.getDate().getTime() - 1000)));
		
		when(item.isAdd()).thenReturn(true);
		when(item.isContent()).thenReturn(true);
		when(item.isContentModified()).thenReturn(false);
		when(item.isProperties()).thenReturn(true);
		when(item.isPropertiesModified()).thenReturn(false);
		
		pathinfo.handle(p);
		
		IndexingDoc f = p.getFields();
		assertEquals("/my/dir/file.txt", f.getFieldValue("path"));
		assertEquals("file.txt", f.getFieldValue("pathname"));
		assertEquals("/my/dir", f.getFieldValue("pathdir"));
		assertEquals("txt", f.getFieldValue("pathext"));
		assertEquals("/svn/repo1/my/dir/file.txt", f.getFieldValue("pathfull"));
		Collection<Object> in = f.getFieldValues("pathin");
		assertEquals(2, in.size());
		Iterator<Object> init = in.iterator();
		assertEquals("/my/dir", init.next());
		assertEquals("/my", init.next());
		Collection<Object> fin = f.getFieldValues("pathfullin");
		assertEquals(4, fin.size());
		Iterator<Object> finit = fin.iterator();
		assertEquals("/svn/repo1/my/dir", finit.next());
		assertEquals("/svn/repo1/my", finit.next());
		assertEquals("/svn/repo1", finit.next());
		assertEquals("/svn", finit.next());
		// everything could be derived with copyFrom with pathfull, but for now we don't have all that analysis in Solr
		Collection<Object> part = f.getFieldValues("pathpart");
		Iterator<Object> partit = part.iterator();
		assertEquals("my", partit.next());
		assertEquals("dir", partit.next());
		assertEquals("file.txt", partit.next());
		assertEquals(3, part.size());
		
		assertEquals('A', f.getFieldValue("pathstat"));
		assertEquals(null, f.getFieldValue("pathstatprop")); // schema comment can be interpreted as "" but it does it matter to search?
		assertEquals("file", f.getFieldValue("type"));
		
		assertEquals(rev.getNumber(), f.getFieldValue("rev"));
		assertEquals(rev.getDate(), f.getFieldValue("revt"));
		assertEquals(rev.getNumber() - 2, f.getFieldValue("revc"));
		assertEquals(new Date(rev.getDate().getTime() - 1000), f.getFieldValue("revct"));
		
		assertEquals("https://h.ost:1080/svn/repo1/my/dir/file.txt", f.getFieldValue("url"));
		assertEquals("/svn/repo1/my/dir/file.txt", f.getFieldValue("urlpath"));
		
		assertEquals("repo1", f.getFieldValue("repo"));
		assertEquals("h.ost:1080", f.getFieldValue("repohost"));
		assertEquals("/svn", f.getFieldValue("repoparent"));
		assertEquals("h.ost:1080/svn/repo1", f.getFieldValue("repoid"));
		// TODO logical ID
		
	}

}
