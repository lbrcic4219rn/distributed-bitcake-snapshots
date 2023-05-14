package app.snapshot_bitcake;

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
	public int getServentId() {
		return serventId;
	}
	public int getRecordedAmount() {
		return recordedAmount;
	}
	public Map<Integer, Integer> getGiveHistory() {
		return giveHistory;
	}
	public Map<Integer, Integer> getGetHistory() {
		return getHistory;
	}
	
	
}
