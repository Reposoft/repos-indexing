package se.repos.indexing.twophases;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

public class ItemContentsMemory implements ItemContentBufferStrategy {

	private CmsContentsReader reader;
	
	@Inject
	public ItemContentsMemory setCmsContentsReader(CmsContentsReader reader) {
		this.reader = reader;
		return this;
	}
	
	@Override
	public ItemContentBuffer getBuffer(CmsRepositoryInspection repository,
			RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		Long size = (Long) pathinfo.getFieldValue("size");
		if (size == null) {
			throw new IllegalStateException("No size information in indexing doc " + path + ". Use a different buffer strategy.");
		}		
		return new BufferInMemory(reader, revision, path, size.intValue());
	}
	
	public static class BufferInMemory implements ItemContentBuffer {
		
		private CmsContentsReader reader;
		private RepoRevision revision;
		private CmsItemPath path;
		private int size;

		private ByteArrayOutputStream buffer = null;
		
		public BufferInMemory(CmsContentsReader reader,
				RepoRevision revision, CmsItemPath path, int size) {
			this.reader = reader;
			this.revision = revision;
			this.path = path;
			this.size = size;
		}
		
		@Override
		public InputStream getContents() {
			if (buffer == null) {
				buffer = new ByteArrayOutputStream(size);
				reader.getContents(revision, path, buffer);
				try {
					buffer.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}

		@Override
		public void destroy() {
			buffer = null;
		}		
		
	}	

}
