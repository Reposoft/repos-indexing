/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import java.io.InputStream;

public class ItemContentBufferDeleted implements ItemContentBuffer {

	@Override
	public InputStream getContents() {
		throw new IllegalStateException("Detected an attempt to retrieve contents for deleted item");
	}

	@Override
	public void destroy() {
	}

}
