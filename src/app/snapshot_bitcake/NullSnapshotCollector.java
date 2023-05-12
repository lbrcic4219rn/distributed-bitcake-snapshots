package app.snapshot_bitcake;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}


	@Override
	public void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult) {}

	@Override
	public void addDoneMessage(int id) {

	}

	@Override
	public void setTerminateNotArrived() {

	}

	@Override
	public void startCollecting() {}

	@Override
	public boolean isCollecting() {
		return false;
	}

}
