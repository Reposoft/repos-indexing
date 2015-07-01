/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.util.Set;

import javax.inject.Inject;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Metadata but not versioned properties about an item,
 * i.e. anything that can be derived from basic path info.
 */
public class HandlerPathinfo implements IndexingItemHandler {

	public static final String TYPE_FILE = "file";
	public static final String TYPE_FOLDER = "folder";
	
	// Must be Strings, otherwise the SolR JSON response will contain "java.lang.Character:A".
	public static final String STAT_ADD = "A";
	public static final String STAT_REPLACE = "R";
	public static final String STAT_MODIFY = "M";
	public static final String STAT_DELETE = "D";
	
	// used to detect special case changeset path = root
	private static final CmsItemPath REPOROOT = null;
	
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
		
		String repopath = repository.getPath();
		CmsItemPath parent;
		
		// Temporary fix for root propset support, maybe not needed with cms-item 2.4.2+
		if (path == REPOROOT) {
			d.setField("path", "");
			d.setField("pathname", "");
			d.setField("pathnamebase", "");
			d.setField("pathext", "");
			d.setField("pathfull", repopath);
			parent = null;
		} else {
			d.setField("path", path.toString());
			d.setField("pathname", path.getName());
			d.setField("pathnamebase", path.getNameBase());
			d.setField("pathext", path.getExtension());
			d.setField("pathfull", repopath + path.toString());
			for (String segment : path.getPathSegments()) {
				d.addField("pathpart", segment);
			}
			parent = path.getParent();
		}
		
		if (parent != null) {
			d.setField("pathdir", parent.toString());
		}
		while (parent != null) {
			d.addField("pathin", parent.toString());
			d.addField("pathfullin", repopath + parent.toString());
			parent = parent.getParent();
		}
		// Non-typical use of CmsItemPath: processing the path above repo.
		CmsItemPath repopathparent = new CmsItemPath(repopath);
		if (path == REPOROOT) {
			repopathparent = repopathparent.getParent();
		}
		while (repopathparent != null) {
			d.addField("pathfullin", repopathparent.getPath());
			repopathparent = repopathparent.getParent();
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
		if (item.isDerived()) {
			// This is incorrect but it is the legacy behavior to set path revision = commit revision for derived items even though they differ
			d.setField("revc", revision.getNumber());
			d.setField("revct", revision.getDate());
		} else {
			d.setField("revc", item.getRevisionChanged().getNumber());
			d.setField("revct", item.getRevisionChanged().getDate());
		}
		
		if (item.isAdd()) {
			d.setField("pathstat", STAT_ADD);
		} else if (item.isReplace()) {
			d.setField("pathstat", STAT_REPLACE);
		} else if (item.isDelete()) {
			d.setField("pathstat", STAT_DELETE);
		} else if (item.isContent()) {
			d.setField("pathstat", STAT_MODIFY);
		}
		
		if (item.isPropertiesModified()) { // NOTE: This is using a different field, which makes propstat empty sometimes.
			d.setField("pathstatprop", STAT_MODIFY);
		}
		
		d.setField("copyhas", item.isCopySource());
		if (item.isCopy()) {
			d.setField("copyfrom", item.getCopyFromPath().toString());
			d.setField("copyfromrev", item.getCopyFromRevision().getNumber());
			d.setField("copyfromrevt", item.getCopyFromRevision().getDate());
		}
		
		CmsItemId url = repository.getItemId().withRelPath(path);
		d.setField("url", url.getUrl());
		d.setField("urlpath", url.getUrlAtHost());
	}
	
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}
	
}
