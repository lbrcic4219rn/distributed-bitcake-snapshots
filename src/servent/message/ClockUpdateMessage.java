package servent.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;

public class ClockUpdateMessage extends BasicMessage {
	

	private static final long serialVersionUID = 1L;
	private Map<Integer, Integer> senderVectorClock;
	
	public ClockUpdateMessage(ServentInfo sender, ServentInfo receiver,Map<Integer, Integer> senderVectorClock) {
	
		super(MessageType.CLOCK_UPDATE,sender,receiver);
		
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	public ClockUpdateMessage(ServentInfo sender, ServentInfo receiver,String messageText,List<ServentInfo> routeList,Map<Integer, Integer> senderVectorClock,int myId) {
		
		super(MessageType.CLOCK_UPDATE,sender,receiver,routeList,messageText,myId);
		
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	
	
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);
		Message toReturn = new ClockUpdateMessage(getOriginalSenderInfo(),
				getReceiverInfo(),getMessageText(),newRouteList, getSenderVectorClock(),getMessageId());
		
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
			
			Message toReturn = new ClockUpdateMessage(getOriginalSenderInfo(),
					newReceiverInfo, getMessageText(),getRoute(), getSenderVectorClock(),getMessageId());
			
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
