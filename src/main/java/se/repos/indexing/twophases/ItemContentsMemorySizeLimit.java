package se.repos.indexing.twophases;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentsBuffer;
import se.repos.indexing.item.ItemContentsBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * 
 * TODO we need a cleanup strategy for temp files
 */
public class ItemContentsMemorySizeLimit implements ItemContentsBufferStrategy {
	
	public static final int DEFAULT_SIZE_LIMIT = 100000;
	
	private CmsContentsReader reader;
	
	private int limit = DEFAULT_SIZE_LIMIT;
	
	@Inject
	public void setFileSizeInMemoryLimit(@Named("indexingFilesizeInMemoryLimitBytes") int bytes) {
		if (bytes > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Can not cache files larger than " + Integer.MAX_VALUE + " in memory");
		}
		this.limit = bytes;
	}
	
	@Inject
	public ItemContentsMemorySizeLimit setCmsContentsReader(CmsContentsReader reader) {
		this.reader = reader;
		return this;
	}	
	
	@Override
	public ItemContentsBuffer getBuffer(CmsRepositoryInspection repository,
			RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		Long size = (Long) pathinfo.getFieldValue("size");
		if (size == null) {
			throw new IllegalStateException("No size information in indexing doc " + path + ". Use a different buffer strategy.");
		}
		if (size <= limit) {
			return new BufferInMemory(repository, revision, path, size.intValue());
		} else {
			return new BufferTempFile(repository, revision, path);
		}
	}
	
	public class BufferInMemory implements ItemContentsBuffer {
		
		private CmsRepositoryInspection repository;
		private RepoRevision revision;
		private CmsItemPath path;
		private int size;

		private ByteArrayOutputStream buffer = null;
		
		public BufferInMemory(CmsRepositoryInspection repository,
				RepoRevision revision, CmsItemPath path, int size) {
			this.repository = repository;
			this.revision = revision;
			this.path = path;
			this.size = size;
		}
		
		@Override
		public InputStream getContents() {
			if (buffer == null) {
				buffer = new ByteArrayOutputStream(size);
				reader.getContents(repository, revision, path, buffer);
				try {
					buffer.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}		
		
	}
	
	/**
	 * Buffer to temp file.
	 */
	public class BufferTempFile implements ItemContentsBuffer {

		private CmsRepositoryInspection repository;
		private RepoRevision revision;
		private CmsItemPath path;

		private File tempfile = null;
		
		public BufferTempFile(CmsRepositoryInspection repository,
				RepoRevision revision, CmsItemPath path) {
			this.repository = repository;
			this.revision = revision;
			this.path = path;
		}

		@Override
		public InputStream getContents() {
			if (tempfile == null) {
				// Found
				// http://ostermiller.org/convert_java_outputstream_inputstream.html
				// https://code.google.com/p/io-tools/wiki/Tutorial_EasyStream
				// But we'd better wait until we start adapting to file size
				try {
					tempfile = File.createTempFile("repos-indexing-contents-buffer", ".tmp");
				} catch (IOException e2) {
					throw new IllegalStateException("Failed to produce temp file destination for contents buffer");
				}
				tempfile.deleteOnExit(); // TODO does this work? Can we get notified on stream close? Do we reuse the file for subsequent reads? TODO anyway it will fill up the disk for long indexing runs
				OutputStream out;
				try {
					out = new FileOutputStream(tempfile);
				} catch (FileNotFoundException e1) {
					throw new IllegalStateException("Failed to write to templ file for contents buffer");
				}
				reader.getContents(repository, revision, path, out);
			}
			try {
				return new FileInputStream(tempfile);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("Failed to produce readable input from temp file");
			}
		}
		
	}	
	
}
