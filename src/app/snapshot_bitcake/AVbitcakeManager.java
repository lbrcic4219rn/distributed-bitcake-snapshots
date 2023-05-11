package app.snapshot_bitcake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.Message;
import servent.message.snapshot.AVdoneMessage;
import servent.message.snapshot.AVtokenMessage;
import servent.message.util.MessageUtil;

public class AVbitcakeManager implements BitcakeManager
{

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();
	
	private AVSnapshotResult avSnapshotResult;
	
	public AVbitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}
		avSnapshotResult = null;
	}
	
	public AVSnapshotResult getAvSnapshotResult() {
		return avSnapshotResult;
	}
	
	public Map<Integer, Integer> getGetHistory() {
		return getHistory;
	}
	
	public Map<Integer, Integer> getGiveHistory() {
		return giveHistory;
	}
	
	public int recordedAmount = 0;
	
	public void tokenEvent(int collectorId, SnapshotCollector snapshotCollector,Map<Integer,Integer> tokenClock) {
		synchronized (AppConfig.colorLock) {
			recordedAmount = getCurrentBitcakeAmount();
			
				avSnapshotResult = new AVSnapshotResult(AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory, tokenClock, collectorId);
				for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
					giveHistory.put(neighbor, 0);
					getHistory.put(neighbor, 0);
				}
				
				
				if (collectorId == AppConfig.myServentInfo.getId()) {
					snapshotCollector.addAVDoneMessages(AppConfig.myServentInfo.getId(), true);
					AppConfig.hasReceivedToken.set(true);

				}
				else {
					
					AppConfig.hasReceivedToken.set(true);
					Message doneMessage = new AVdoneMessage(
							AppConfig.myServentInfo, null, collectorId,CausalBroadcastShared.getVectorClock());
					
					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
					{
						
						MessageUtil.sendMessage(doneMessage.changeReceiver(neighbor));
					}
					CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());
					
				}
				
				if(collectorId == AppConfig.myServentInfo.getId())
				{
					Message AvTokenMessage = new AVtokenMessage(AppConfig.myServentInfo, null, collectorId,CausalBroadcastShared.getVectorClock());
					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
					{
						
						MessageUtil.sendMessage(AvTokenMessage.changeReceiver(neighbor));
					}
					CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());
				}
			
		}
		
	}
	
	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}
	
	public void recordGiveTransaction(int neighbor, int amount) {
		giveHistory.compute(neighbor, new MapValueUpdater(amount));
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		getHistory.compute(neighbor, new MapValueUpdater(amount));
	}

}
