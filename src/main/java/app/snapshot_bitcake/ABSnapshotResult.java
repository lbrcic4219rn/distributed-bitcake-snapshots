package app.snapshot_bitcake;

import lombok.Getter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is recordedAmount,
 * and messages from others to that servent are recorded in
 * allChannelMessages.
 * 
 * @author bmilojkovic
 *
 */
@Getter
public class ABSnapshotResult implements Serializable {

	private final int serventId;
	private final int recordedAmount;
	private final Map<Integer, Integer> giveHistory;
	private final Map<Integer, Integer> getHistory;

	public ABSnapshotResult(int serventId, int recordedAmount,
							Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory) {
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.giveHistory = new ConcurrentHashMap<>(giveHistory);
		this.getHistory = new ConcurrentHashMap<>(getHistory);
	}
	
	
}
