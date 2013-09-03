package se.repos.indexing.item;

import java.io.InputStream;

public class ItemContentsBufferDeleted implements ItemContentsBuffer {

	@Override
	public InputStream getContents() {
		throw new IllegalStateException("Attempt to retrieve contents for deleted item");
	}

}
