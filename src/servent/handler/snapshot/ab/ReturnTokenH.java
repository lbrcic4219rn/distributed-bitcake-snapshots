package servent.handler.snapshot.ab;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.ABreturnTokenMessage;

public class ReturnTokenH implements MessageHandler{
	
	private Message newMessage;

	public ReturnTokenH(Message newMessage) {
		this.newMessage = newMessage;
	}

	@Override
	public void run() {
		if(AppConfig.myServentInfo.getId() == Integer.parseInt(newMessage.getMessageText()))
		{
			ABreturnTokenMessage abReturnTokenMessage = (ABreturnTokenMessage)newMessage;
			CausalBroadcastShared.snapshotCollector.addABSnapshotInfo(abReturnTokenMessage.getOriginalSenderInfo().getId(),
												abReturnTokenMessage.getABSnapshotResult());
		}
		
	}

}
