package se.repos.indexing;

public interface CoreCommit {

	/**
	 * Called for explicitly requested commit.
	 */
	void commit();
	
	/**
	 * Called for explicitly requested optimize.
	 */
	void optimize();
	
	/**
	 * Called when a doc or batch of docs is sent to index.
	 */
	void onDocAdd(int batchSize);
	
	/**
	 * Called when all items in a revision have been processed
	 */
	void onCompleteRevision();
	
	/**
	 * Called when all revisions in a sync request have been processed.
	 */
	void onCompleteSync();
	
}
