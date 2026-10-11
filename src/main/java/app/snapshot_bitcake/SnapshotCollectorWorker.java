package app.snapshot_bitcake;

import app.AppConfig;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main snapshot collector class. Has support for Acharya-Badrinath
 * and Alagar-Venkatesan snapshot algorithms.
 *
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

    private final AtomicBoolean collecting = new AtomicBoolean(false);
    private final Map<Integer, ABSnapshotResult> collectedABValues = new ConcurrentHashMap<>();
    private final List<Integer> collectedDoneMessages = new CopyOnWriteArrayList<>();
    private final SnapshotType snapshotType;
    private volatile boolean working = true;
    private volatile boolean terminateNotArrived = true;
    private BitcakeManager bitcakeManager;

    public SnapshotCollectorWorker(SnapshotType snapshotType) {
        this.snapshotType = snapshotType;

        switch (snapshotType) {
            case ACHARYA_BADRINATH:
                bitcakeManager = new AcharyaBadrinathBitcakeManager();
                break;
            case ALAGAR_VENKATESAN:
                bitcakeManager = new AlagarVenkatesanBitcakeManager();
                break;
            case NONE:
                AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
                System.exit(0);
        }
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return bitcakeManager;
    }

    /*
     * Collecting is done in three stages:
     * 1. Send messages asking for values
     * 2. Wait for all the responses
     * 3. Print result
     */
    @Override
    public void run() {
        while (working) {
            if (!waitUntilCollecting()) {
                return;
            }

            sendAsks();

            if (!waitForResponses()) {
                return;
            }

            if (!printResult()) {
                return;
            }

            collecting.set(false);
        }
    }

    /**
     * Sleeps for a second.
     *
     * @return true if we were stopped (or interrupted) and should finish.
     */
    private boolean stoppedWhileSleeping() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }

        return !working;
    }

    /**
     * Not collecting yet - just sleep until we start actual work, or finish.
     */
    private boolean waitUntilCollecting() {
        while (!collecting.get()) {
            if (stoppedWhileSleeping()) {
                return false;
            }
        }

        return true;
    }

    private void sendAsks() {
        switch (snapshotType) {
            case ACHARYA_BADRINATH:
                ((AcharyaBadrinathBitcakeManager) bitcakeManager).sendToken(this);
                break;

            case ALAGAR_VENKATESAN:
                ((AlagarVenkatesanBitcakeManager) bitcakeManager).sendToken();
                break;

            case NONE:
                //Shouldn't be able to come here. See constructor.
                break;
        }
    }

    private boolean waitForResponses() {
        boolean waiting = true;
        while (waiting) {
            switch (snapshotType) {
                case ACHARYA_BADRINATH:
                    if (collectedABValues.size() == AppConfig.getServentCount()) {
                        waiting = false;
                    }
                    break;
                case ALAGAR_VENKATESAN:
                    if (collectedDoneMessages.size() + 1 == AppConfig.getServentCount()) {
                        waiting = false;
                        ((AlagarVenkatesanBitcakeManager) bitcakeManager).sendTerminate(this);
                    }
                    break;
                case NONE:
                    //Shouldn't be able to come here. See constructor.
                    break;
            }

            if (stoppedWhileSleeping()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return false if we were stopped while waiting and should finish.
     */
    private boolean printResult() {
        switch (snapshotType) {
            case ACHARYA_BADRINATH:
                printABResult();
                collectedABValues.clear(); //reset for next invocation
                break;

            case ALAGAR_VENKATESAN:
                //Every node prints its own result when the terminate message gets to it.
                while (terminateNotArrived) {
                    if (stoppedWhileSleeping()) {
                        return false;
                    }
                }
                collectedDoneMessages.clear();
                break;

            case NONE:
                //Shouldn't be able to come here. See constructor.
                break;
        }

        return true;
    }

    private void printABResult() {
        int sum = 0;
        for (Entry<Integer, ABSnapshotResult> nodeResult : collectedABValues.entrySet()) {
            sum += nodeResult.getValue().recordedAmount();
            AppConfig.timestampedStandardPrint(
                    "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().recordedAmount());
        }

        for (int i = 0; i < AppConfig.getServentCount(); i++) {
            for (int j = 0; j < AppConfig.getServentCount(); j++) {
                sum += printUnreceived(i, j);
            }
        }

        AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
    }

    /**
     * @return the amount servent i sent to servent j that j hasn't recorded as received.
     */
    private int printUnreceived(int i, int j) {
        boolean neighbors = i != j
                && AppConfig.getInfoById(i).neighbors().contains(j)
                && AppConfig.getInfoById(j).neighbors().contains(i);
        if (!neighbors) {
            return 0;
        }

        int ijAmount = collectedABValues.get(i).giveHistory().get(j);
        int jiAmount = collectedABValues.get(j).getHistory().get(i);

        if (ijAmount != jiAmount) {
            String outputString = String.format(
                    "Unreceived bitcake amount: %d from servent %d to servent %d",
                    ijAmount - jiAmount, i, j);
            AppConfig.timestampedStandardPrint(outputString);
        }

        return ijAmount - jiAmount;
    }

    @Override
    public void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult) {
        collectedABValues.put(id, abSnapshotResult);
    }

    @Override
    public void addDoneMessage(int id) {
        collectedDoneMessages.add(id);
    }

    @Override
    public void startCollecting() {
        terminateNotArrived = true;
        boolean oldValue = this.collecting.getAndSet(true);

        if (oldValue) {
            AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
        }
    }

    @Override
    public boolean isCollecting() {
        return collecting.get();
    }

    @Override
    public void stop() {
        working = false;
    }

    public void setTerminateNotArrived() {
        terminateNotArrived = false;
    }

}
