package servent.message.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class ABtokenMessage extends BasicMessage{
	

	private static final long serialVersionUID = 1L;
	private Map<Integer, Integer> senderVectorClock;

	public ABtokenMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.AB_TOKEN, sender, receiver, String.valueOf(collectorId));
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	public ABtokenMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> senderVectorClock,List<ServentInfo> routeList,int myId) {
		super(MessageType.AB_TOKEN, sender, receiver, routeList, String.valueOf(collectorId),myId);
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);
		Message toReturn = new ABtokenMessage(getOriginalSenderInfo(),
				getReceiverInfo(),Integer.parseInt(getMessageText()), getSenderVectorClock(),newRouteList,getMessageId());
		
		return toReturn;
	}
	
	/**
	 * Change the message received based on ID. The receiver has to be our neighbor.
	 * Use this when you want to send a message to multiple neighbors, or when resending.
	 */
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			
			Message toReturn = new ABtokenMessage(getOriginalSenderInfo(),
					newReceiverInfo,Integer.parseInt(getMessageText()) , getSenderVectorClock(),getRoute(),getMessageId());
			
			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			
			return null;
		}
		
	}
	
	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}

}
