package servent.message.snapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class AVdoneMessage extends BasicMessage
{

	private static final long serialVersionUID = 1L;
	private Map<Integer, Integer> senderVectorClock;
	
	public AVdoneMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> senderVectorClock)
	{
		super(MessageType.AV_DONE, sender, receiver, String.valueOf(collectorId));
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	public AVdoneMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> senderVectorClock,List<ServentInfo> routeList,int myId) {
		super(MessageType.AV_DONE, sender, receiver, routeList, String.valueOf(collectorId),myId);
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			
			Message toReturn = new AVdoneMessage(getOriginalSenderInfo(),
					newReceiverInfo,Integer.parseInt(getMessageText()) , getSenderVectorClock(),getRoute(),getMessageId());
			
			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			
			return null;
		}
		
	}
	
	
	public Map<Integer, Integer> getSenderVectorClock() 
	{
		return senderVectorClock;
	}
	

}
