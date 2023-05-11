package servent.handler.snapshot.ab;

import app.CausalBroadcastShared;
import app.snapshot_bitcake.ABbitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;

public class TokenH implements MessageHandler{
	
	private Message newMessage;

	public TokenH(Message newMessage) {
		this.newMessage = newMessage;
	}
	
	@Override
	public void run() {
		ABbitcakeManager abBitcakeManager = (ABbitcakeManager)CausalBroadcastShared.snapshotCollector.getBitcakeManager();
		abBitcakeManager.tokenEvent(Integer.parseInt(newMessage.getMessageText()), CausalBroadcastShared.snapshotCollector);
		
	}

}
