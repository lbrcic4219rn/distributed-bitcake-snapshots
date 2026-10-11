package app.snapshot_bitcake;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 *
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

    @Override
    public void run() {
        //No snapshot algorithm selected, so there is nothing to do.
    }

    @Override
    public void stop() {
        //No snapshot algorithm selected, so there is nothing to do.
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return null;
    }

    @Override
    public void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult) {
        //No snapshot algorithm selected, so there is nothing to do.
    }

    @Override
    public void addDoneMessage(int id) {
        //No snapshot algorithm selected, so there is nothing to do.
    }

    @Override
    public void setTerminateNotArrived() {
        //No snapshot algorithm selected, so there is nothing to do.
    }


    @Override
    public void startCollecting() {
        //No snapshot algorithm selected, so there is nothing to do.
    }

    @Override
    public boolean isCollecting() {
        return false;
    }

}
