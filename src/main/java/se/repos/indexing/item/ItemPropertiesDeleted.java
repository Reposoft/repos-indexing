package se.repos.indexing.item;

import java.util.List;
import java.util.Set;

import se.simonsoft.cms.item.properties.CmsItemProperties;

public class ItemPropertiesDeleted implements CmsItemProperties {

	@Override
	public boolean containsProperty(String key) {
		throw new IllegalStateException("Attempt to retrieve properties for deleted item");
	}

	@Override
	public Set<String> getKeySet() {
		throw new IllegalStateException("Attempt to retrieve properties for deleted item");
	}

	@Override
	public String getString(String key) {
		throw new IllegalStateException("Attempt to retrieve properties for deleted item");
	}

	@Override
	public List<String> getList(String key) throws ClassCastException {
		throw new IllegalStateException("Attempt to retrieve properties for deleted item");
	}

}
