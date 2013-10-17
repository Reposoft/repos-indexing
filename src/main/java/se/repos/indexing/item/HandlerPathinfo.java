/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import javax.inject.Inject;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

/**
 * Metadata but not versioned properties about an item,
 * i.e. anything that can be derived from basic path info.
 */
public class HandlerPathinfo implements IndexingItemHandler {

	private static final String URLENCODE_CHARSET = "UTF-8";
	public static final String TYPE_FILE = "file";
	public static final String TYPE_FOLDER = "folder";
	
	public static final char STAT_ADD = 'A';
	public static final char STAT_REPLACE = 'R';
	public static final char STAT_MODIFY = 'M';
	public static final char STAT_DELETE = 'D';
	
	private IdStrategy idStrategy;
	
	@Inject
	public void setIdStrategy(IdStrategy idStrategy) {
		this.idStrategy = idStrategy;
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem item = progress.getItem();
		IndexingDoc d = progress.getFields();
		
		CmsRepository repository = progress.getRepository();
		RepoRevision revision = progress.getRevision();
		CmsItemPath path = item.getPath();
		
		d.setField("id", idStrategy.getId(repository, revision, path));
		d.setField("idhead", idStrategy.getIdHead(repository, path));
		
		d.setField("repo", repository.getName());
		d.setField("repoparent", repository.getParentPath());
		d.setField("repohost", repository.getHost());
		d.setField("repoid", idStrategy.getIdRepository(repository));
		
		d.setField("path", path.toString());
		d.setField("pathname", path.getName());
		d.setField("pathext", path.getExtension());
		
		String repopath = repository.getPath();
		d.setField("pathfull", repopath + path.toString());
		CmsItemPath parent = path.getParent();
		if (parent != null) {
			d.setField("pathdir", parent.toString());
		}
		while (parent != null) {
			d.addField("pathin", parent.toString());
			d.addField("pathfullin", repopath + parent.toString());
			parent = parent.getParent();
		}
		CmsItemPath repopathparent = new CmsItemPath(repopath);
		while (repopathparent != null) {
			d.addField("pathfullin", repopathparent.getPath());
			repopathparent = repopathparent.getParent();
		}
		for (String segment : path.getPathSegments()) {
			d.addField("pathpart", segment);
		}
		
		if (item.isFile()) {
			d.setField("type", TYPE_FILE);
			if (!item.isDelete()) {
				d.setField("size", item.getFilesize());
			}
		}
		if (item.isFolder()) {
			d.setField("type", TYPE_FOLDER);
		}
		
		d.setField("rev", revision.getNumber());
		d.setField("revt", revision.getDate());
		d.setField("revc", item.getRevisionChanged().getNumber());
		d.setField("revct", item.getRevisionChanged().getDate());
		
		if (item.isAdd()) {
			d.setField("pathstat", STAT_ADD);
		} else if (item.isReplace()) {
			d.setField("pathstat", STAT_REPLACE);
		} else if (item.isDelete()) {
			d.setField("pathstat", STAT_DELETE);
		} else if (item.isContent()) {
			d.setField("pathstat", STAT_MODIFY);
		}
		
		if (item.isPropertiesModified()) {
			d.setField("pathstatprop", STAT_MODIFY);
		}
		
		if (item.isCopy()) { // TODO is this flag set for deletions that are copy-from sources in the same commit? no- do we need hasCopy?
			d.setField("copyhas", true); // or pathcopyhas?
		}
		
		String pathurl = urlencode(path);
		d.setField("url", repository.getUrl() + pathurl);
		d.setField("urlpath", repository.getUrlAtHost() + pathurl);
	}
	
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}
	
	private String urlencode(CmsItemPath path) {
		StringBuffer b = new StringBuffer();
		for (String p : path.getPathSegments()) {
			b.append('/').append(urlencode(p));
		}
		return b.toString();
	}
	
	private String urlencode(String str) {
		try {
			return URLEncoder.encode(str, URLENCODE_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Server does not support charset " + URLENCODE_CHARSET, e);
		}
	}
	
}
