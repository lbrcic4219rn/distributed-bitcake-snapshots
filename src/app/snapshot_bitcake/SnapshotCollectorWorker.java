package app.snapshot_bitcake;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import app.AppConfig;



/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;

	private volatile boolean terminateNotArrived = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);

	private Map<Integer, ABSnapshotResult> collectedABValues = new ConcurrentHashMap<>();
	private List<Integer> collectedDoneMessages = new CopyOnWriteArrayList<>();

	private SnapshotType snapshotType;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
		
		switch(snapshotType) {
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
					// TODO Auto-generated catch block
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
			case ACHARYA_BADRINATH:
				((AcharyaBadrinathBitcakeManager)bitcakeManager).sendToken(this);
				break;
			case ALAGAR_VENKATESAN:
				((AlagarVenkatesanBitcakeManager)bitcakeManager).sendToken();
				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case ACHARYA_BADRINATH:
					if (collectedABValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case ALAGAR_VENKATESAN:
					if (collectedDoneMessages.size() +1 == AppConfig.getServentCount()) {
						waiting = false;
						((AlagarVenkatesanBitcakeManager)bitcakeManager).sendTerminate(this);
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
			case ACHARYA_BADRINATH:
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

				collectedABValues.clear(); //reset for next invocation
				break;
			case ALAGAR_VENKATESAN:
				while(terminateNotArrived);
				{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				collectedDoneMessages.clear();
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			collecting.set(false);
		}

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
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
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

	public void setTerminateNotArrived(){
		terminateNotArrived = false;
	}

}
