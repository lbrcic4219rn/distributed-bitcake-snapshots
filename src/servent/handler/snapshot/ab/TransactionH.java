package servent.handler.snapshot.ab;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.ABbitcakeManager;
import app.snapshot_bitcake.AVbitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.TransactionMessage;

public class TransactionH implements MessageHandler {
	
	private Message newMessage;
	
	public TransactionH(Message newMessage) {
		this.newMessage = newMessage;
	}

	@Override
	public void run() {
		
String amountString = newMessage.getMessageText();
		
		int amountNumber = 0;
		try {
			amountNumber = Integer.parseInt(amountString);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
			return;
		}
		

		synchronized (AppConfig.colorLock) 
		{
			
			CausalBroadcastShared.snapshotCollector.getBitcakeManager().addSomeBitcakes(amountNumber);
			
			if (CausalBroadcastShared.snapshotCollector.getBitcakeManager() instanceof ABbitcakeManager) 
			{
				ABbitcakeManager abBitcakeManager = (ABbitcakeManager)CausalBroadcastShared.snapshotCollector.getBitcakeManager();
				abBitcakeManager.recordGetTransaction(newMessage.getOriginalSenderInfo().getId(), amountNumber);
			}
			if(CausalBroadcastShared.snapshotCollector.getBitcakeManager() instanceof AVbitcakeManager)
			{
				AVbitcakeManager avBitcakeManager = (AVbitcakeManager)CausalBroadcastShared.snapshotCollector.getBitcakeManager();
				if(AppConfig.hasReceivedToken.get() == false)
				{
					avBitcakeManager.recordGetTransaction(newMessage.getOriginalSenderInfo().getId(), amountNumber);
				}
				else
				{
					int sId = avBitcakeManager.getAvSnapshotResult().getSenderId();
					TransactionMessage transactionMessage = (TransactionMessage)newMessage;
					if(transactionMessage.getSenderVectorClock().get(sId) <= avBitcakeManager.getAvSnapshotResult().getTokenClock().get(sId))
					{
						avBitcakeManager.recordGetTransaction(transactionMessage.getOriginalSenderInfo().getId(), amountNumber);
					}
					
				}
				
			}
		}
		
		
	}

}
