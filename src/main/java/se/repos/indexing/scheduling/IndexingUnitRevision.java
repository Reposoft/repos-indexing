package se.repos.indexing.scheduling;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;

/**
 * All changeset items in a single revision from a repository.
 * 
 * Scheduling can verify that {@link IndexingUnit} is of this type wherever such an assumption is needed.
 */
public class IndexingUnitRevision extends IndexingUnit {

	private CmsRepository repository = null;
	private RepoRevision revision = null;
	
	public IndexingUnitRevision(Iterable<IndexingItemProgress> items,
			Iterable<IndexingItemHandler> handler) {
		super(items, handler);
		for (IndexingItemProgress i : items) {
			if (repository == null) {
				repository = i.getRepository();
				revision = i.getRevision();
				if (repository == null) {
					throw new IllegalArgumentException("Missing reository in item " + i);
				}
				if (revision == null) {
					throw new IllegalArgumentException("Missing revision in item " + i);
				}
			} else {
				if (!repository.equals(i.getRepository())) {
					throw new IllegalArgumentException("All indexing items must be from the same repository. Got " + repository + " then " + i.getRepository());
				}
				if (!revision.equals(i.getRevision())) {
					throw new IllegalArgumentException("All indexing items must be from the same revision. Got " + revision + " then " + i.getRevision());
				}
			}
		}
	}

}
