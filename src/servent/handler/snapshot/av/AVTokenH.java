package servent.handler.snapshot.av;


import app.CausalBroadcastShared;
import app.snapshot_bitcake.AVbitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.AVtokenMessage;

public class AVTokenH implements MessageHandler
{
	private Message newMessage;

	public AVTokenH(Message newMessage) {
		this.newMessage = newMessage;
	}
	
	@Override
	public void run() 
	{
		AVtokenMessage tokenMessage = (AVtokenMessage)newMessage;
		AVbitcakeManager avBitcakeManager = (AVbitcakeManager)CausalBroadcastShared.snapshotCollector.getBitcakeManager();
		avBitcakeManager.tokenEvent(Integer.parseInt(tokenMessage.getMessageText()), CausalBroadcastShared.snapshotCollector,tokenMessage.getSenderVectorClock());
	
	}

}
