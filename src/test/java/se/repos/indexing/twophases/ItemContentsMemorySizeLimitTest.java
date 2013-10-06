package se.repos.indexing.twophases;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Test;

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

public class ItemContentsMemorySizeLimitTest {

	@Test
	public void test() {
		CmsContentsReader reader = mock(CmsContentsReader.class);
		ItemContentsMemorySizeLimit buffer = new ItemContentsMemorySizeLimit();
		buffer.setCmsContentsReader(reader);
		buffer.setFileSizeInMemoryLimit(100);
		
		RepoRevision rev = new RepoRevision(1, new Date());
		//buffer.getBuffer(null, revision, path, pathinfo)
	}

}
