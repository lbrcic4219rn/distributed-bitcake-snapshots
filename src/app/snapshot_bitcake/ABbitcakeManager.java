package app.snapshot_bitcake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.Message;
import servent.message.snapshot.ABreturnTokenMessage;
import servent.message.snapshot.ABtokenMessage;
import servent.message.util.MessageUtil;

public class ABbitcakeManager implements BitcakeManager {

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
	
	public ABbitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void tokenEvent(int collectorId, SnapshotCollector snapshotCollector) {
		synchronized (AppConfig.colorLock) {
			recordedAmount = getCurrentBitcakeAmount();

			ABSnapshotResult snapshotResult = new ABSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);
			
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addABSnapshotInfo(
						AppConfig.myServentInfo.getId(),
						snapshotResult);
			} else {
			
				Message abReturnTokenMessage = new ABreturnTokenMessage(
						AppConfig.myServentInfo, null, snapshotResult,CausalBroadcastShared.getVectorClock(),collectorId);
				
				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
				{
					
					MessageUtil.sendMessage(abReturnTokenMessage.changeReceiver(neighbor));
				}
				CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());
				
			}
			
			if(collectorId == AppConfig.myServentInfo.getId())
			{
				Message tokenMessage = new ABtokenMessage(AppConfig.myServentInfo, null, collectorId,CausalBroadcastShared.getVectorClock());
				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
				{
					
					MessageUtil.sendMessage(tokenMessage.changeReceiver(neighbor));
//					try {
//						/**
//						 * This sleep is here to artificially produce some white node -> red node messages
//						 */
//						Thread.sleep(100);
//					} 
//					catch (InterruptedException e) 
//					{
//						e.printStackTrace();
//					}
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
