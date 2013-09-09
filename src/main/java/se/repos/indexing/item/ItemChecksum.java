/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.io.IOException;
import java.util.Set;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.Checksum.Algorithm;
import se.simonsoft.cms.item.impl.ChecksumRead;

public class ItemChecksum implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		if (!progress.getItem().isFile()) {
			return;
		}
		ChecksumRead checksum = new ChecksumRead(Algorithm.MD5, Algorithm.SHA1, Algorithm.SHA256);
		try {
			checksum.add(progress.getContents());
		} catch (IOException e) {
			throw new RuntimeException("Error not handled", e);
		}
		IndexingDoc doc = progress.getFields();
		for (Algorithm a : Checksum.Algorithm.values()) {
			String c = checksum.getHex(a);
			if (c != null) {
				doc.addField("checksum_" + a.toString().toLowerCase(), c);
			}
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

}
