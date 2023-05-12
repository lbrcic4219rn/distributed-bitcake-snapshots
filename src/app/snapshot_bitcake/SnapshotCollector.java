package app.snapshot_bitcake;

import app.Cancellable;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();


	void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult);
	void addDoneMessage(int id);
	void setTerminateNotArrived();
	void startCollecting();
	boolean isCollecting();

}