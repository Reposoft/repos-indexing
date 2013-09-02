package se.repos.indexing;

public interface IndexingEventHandler {

	void onDocAdd(int batchSize);
	
}
