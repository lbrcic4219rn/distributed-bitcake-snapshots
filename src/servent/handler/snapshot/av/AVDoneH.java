package servent.handler.snapshot.av;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.handler.MessageHandler;
import servent.message.Message;

public class AVDoneH implements MessageHandler
{
	private Message newMessage;

	public AVDoneH(Message newMessage) {
		this.newMessage = newMessage;
	}
	
	@Override
	public void run() 
	{
		if(AppConfig.myServentInfo.getId() == Integer.parseInt(newMessage.getMessageText()))
		{
			CausalBroadcastShared.snapshotCollector.addAVDoneMessages(newMessage.getOriginalSenderInfo().getId(), true);
		}
	}

}
