package servent.handler.snapshot.av;

import java.util.Map.Entry;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.AVbitcakeManager;
import servent.handler.MessageHandler;

public class AVTerminateH implements MessageHandler
{

	public AVTerminateH() {}
	
	@Override
	public void run() 
	{
		synchronized (AppConfig.colorLock) {
			
			int sum;
			AVbitcakeManager avBitcakeManager = (AVbitcakeManager)CausalBroadcastShared.snapshotCollector.getBitcakeManager();
			sum = avBitcakeManager.getAvSnapshotResult().getRecordedAmount();
			
			
			for(Entry<Integer,Integer> getAmount: avBitcakeManager.getGetHistory().entrySet())
			{
				sum += getAmount.getValue();
			}
			
			for(Entry<Integer,Integer> giveAmount: avBitcakeManager.getGiveHistory().entrySet())
			{
				sum -= giveAmount.getValue();
			}
			
			AppConfig.timestampedStandardPrint("Node bitcake count: " + sum);
			
			
		}

	}

}
