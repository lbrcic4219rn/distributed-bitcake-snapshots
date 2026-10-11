package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Map;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is recordedAmount,
 * and messages from others to that servent are recorded in
 * allChannelMessages.
 *
 * @author bmilojkovic
 *
 */
public record ABSnapshotResult(int serventId, int recordedAmount, Map<Integer, Integer> giveHistory,
                               Map<Integer, Integer> getHistory) implements Serializable {

    /**
     * The histories are copied, so the result stays as it was at the moment of the snapshot.
     * Without the copy the message would carry the live maps, which keep changing until it is sent.
     */
    public ABSnapshotResult {
        giveHistory = Map.copyOf(giveHistory);
        getHistory = Map.copyOf(getHistory);
    }
}
