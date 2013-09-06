package se.repos.indexing.item;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;

public class ItemPropertiesTest {

	@Test
	public void testHandle() {
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = mock(IndexingDoc.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap();
		props.put("a:prop", "val");
		props.put("another:prop", "multi\nline");
		when(p.getFields()).thenReturn(doc);
		when(p.getProperties()).thenReturn(props);
		
		IndexingItemHandler handler = new ItemProperties();
		handler.handle(p);
		verify(doc).setField("prop_a.prop", "val");
		verify(doc).setField("prop_another.prop", "multi\nline");
	}

}
