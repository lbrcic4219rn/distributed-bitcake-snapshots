package app.snapshot_bitcake;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.handler.snapshot.av.AVTerminateH;
import servent.message.Message;
import servent.message.snapshot.AVTerminateMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	private Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();
	private Map<Integer, ABSnapshotResult> collectedABValues = new ConcurrentHashMap<>();
	private Map<Integer,Boolean> collectedDoneMessages = new ConcurrentHashMap<>();
	
	private SnapshotType snapshotType;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
		
		switch(snapshotType) {		
		case AB_SNAPSHOT:
			bitcakeManager = new ABbitcakeManager();
			break;
		case AV_SNAPSHOT:
			bitcakeManager = new AVbitcakeManager();
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
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			switch (snapshotType) {
			case AB_SNAPSHOT:
				((ABbitcakeManager)bitcakeManager).tokenEvent(AppConfig.myServentInfo.getId(), this);
				break;
			case AV_SNAPSHOT:
				((AVbitcakeManager)bitcakeManager).tokenEvent(AppConfig.myServentInfo.getId(), this,CausalBroadcastShared.getVectorClock());
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				
				case AB_SNAPSHOT:
					if(collectedABValues.size() == AppConfig.getServentCount())
					{
						waiting = false;
					}
					break;
				case AV_SNAPSHOT:
					if(collectedDoneMessages.size() == AppConfig.getServentCount())
					{
						waiting = false;
					}
					break;
				case NONE:
					//Shouldn't be able to come here. See constructor. 
					break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			//print
			int sum;
			switch (snapshotType) {
			
			case AB_SNAPSHOT:
				sum = 0;
				for (Entry<Integer, ABSnapshotResult> nodeResult : collectedABValues.entrySet()) {
					sum += nodeResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
				}
				for(int i = 0; i < AppConfig.getServentCount(); i++) {
					for (int j = 0; j < AppConfig.getServentCount(); j++) {
						if (i != j) {
							if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
								AppConfig.getInfoById(j).getNeighbors().contains(i)) {
								int ijAmount = collectedABValues.get(i).getGiveHistory().get(j);
								int jiAmount = collectedABValues.get(j).getGetHistory().get(i);
								
								if (ijAmount != jiAmount) {
									String outputString = String.format(
											"Unreceived bitcake amount: %d from servent %d to servent %d",
											ijAmount - jiAmount, i, j);
									AppConfig.timestampedStandardPrint(outputString);
									sum += ijAmount - jiAmount;
								}
							}
						}
					}
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
				
				collectedABValues.clear();
				break;
			case AV_SNAPSHOT:
				Message terminateMessage = new AVTerminateMessage(
						AppConfig.myServentInfo, null,CausalBroadcastShared.getVectorClock());
				
				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
				{
					MessageUtil.sendMessage(terminateMessage.changeReceiver(neighbor));
				}
				CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());
				CausalBroadcastShared.handlerThreadPool.submit(new AVTerminateH());
				collectedDoneMessages.clear();
				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			collecting.set(false);
		}

	}
	
	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
		collectedNaiveValues.put(snapshotSubject, amount);
	}

	
	@Override
	public void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult) {
		collectedABValues.put(id, abSnapshotResult);
		
	}
	
	@Override
	public void addAVDoneMessages(int id, Boolean received) {
		collectedDoneMessages.put(id, received);
		
	}
	
	
	
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

	

	

}
