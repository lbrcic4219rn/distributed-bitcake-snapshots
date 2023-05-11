package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class AVTerminateHandler implements MessageHandler
{
	
	private Message clientMessage;
	
	public AVTerminateHandler(Message clientMessage) {
		this.clientMessage = clientMessage; 
	}

	@Override
	public void run() 
	{
		if(clientMessage.getMessageType() == MessageType.AV_TERMINATE)
		{
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			if(senderInfo.getId() == AppConfig.myServentInfo.getId())
			{
				AppConfig.timestampedStandardPrint("Got my own message no rebroadcast.");
			}
			else
			{
				boolean didPut = AppConfig.getReceivedBroadcasts().add(clientMessage);
				
				if(didPut)
				{
					CausalBroadcastShared.addPendingMessage(clientMessage);
					CausalBroadcastShared.checkPendingMessages();
					
					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
					{
						
						MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor));
					}
				}
				
			}
		}
		else
		{
			AppConfig.timestampedErrorPrint("AV_TERMINATE handler got: " + clientMessage);
		}
		
	}

}
