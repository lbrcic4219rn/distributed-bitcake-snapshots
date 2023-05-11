package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AVSnapshotResult implements Serializable
{
	
	private static final long serialVersionUID = 1L;
	
	private final int serventId;
	private final int recordedAmount;
	private final Map<Integer, Integer> giveHistory;
	private final Map<Integer, Integer> getHistory;
	private final Map<Integer, Integer> tokenClock;
	private final int senderId;
	
	public AVSnapshotResult(int serventId, int recordedAmount,
			Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory,
			Map<Integer, Integer> tokenClock,int senderId) {
		
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.giveHistory = new ConcurrentHashMap<>(giveHistory);
		this.getHistory = new ConcurrentHashMap<>(getHistory);
		this.tokenClock = new ConcurrentHashMap<>(tokenClock);
		this.senderId = senderId;
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
	
	public Map<Integer, Integer> getTokenClock() {
		return tokenClock;
	}
	
	public int getSenderId() {
		return senderId;
	}

}
