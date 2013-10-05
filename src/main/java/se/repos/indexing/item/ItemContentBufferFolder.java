/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.io.InputStream;

public class ItemContentBufferFolder implements ItemContentBuffer {

	@Override
	public InputStream getContents() {
		throw new IllegalStateException("Contents retrieval attempted on a folder");
	}

	@Override
	public void destroy() {
	}

}
