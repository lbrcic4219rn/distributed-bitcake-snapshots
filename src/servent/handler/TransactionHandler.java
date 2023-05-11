package servent.handler;


import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import servent.message.ClockUpdateMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;


public class TransactionHandler implements MessageHandler {

	private Message clientMessage;;
	
	public TransactionHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {

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
						
						TransactionMessage trm = (TransactionMessage)clientMessage;
						
						Message clockUpdateMessage = new ClockUpdateMessage(trm.getOriginalSenderInfo(), null,"",trm.getRoute(),trm.getSenderVectorClock(),trm.getMessageId());
						
						AppConfig.getReceivedBroadcasts().add(clockUpdateMessage);
						
						for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) 
						{
							MessageUtil.sendMessage(clockUpdateMessage.changeReceiver(neighbor));
						}
					}
					
				}
			
				
						
			
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
