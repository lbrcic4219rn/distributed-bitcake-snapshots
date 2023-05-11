package servent.message.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class ABreturnTokenMessage extends BasicMessage {
	
	
	private static final long serialVersionUID = 1L;
	private ABSnapshotResult abSnapshotResult;
	private Map<Integer, Integer> senderVectorClock;

	
	public ABreturnTokenMessage(ServentInfo sender, ServentInfo receiver, ABSnapshotResult abSnapshotResult,Map<Integer, Integer> senderVectorClock,int collectorId) {
		super(MessageType.AB_RETURN_TOKEN, sender, receiver,String.valueOf(collectorId));
		
		this.abSnapshotResult = abSnapshotResult;
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	
	}
	
	public ABreturnTokenMessage(ServentInfo sender, ServentInfo receiver, ABSnapshotResult abSnapshotResult,Map<Integer, Integer> senderVectorClock,int collectorId,List<ServentInfo> routeList,int myId) {
		super(MessageType.AB_RETURN_TOKEN, sender, receiver,routeList,String.valueOf(collectorId),myId);

		this.abSnapshotResult = abSnapshotResult;
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	
	}
	
	
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);
		Message toReturn = new ABreturnTokenMessage(getOriginalSenderInfo(),
				getReceiverInfo(),getABSnapshotResult(), getSenderVectorClock(),Integer.parseInt(getMessageText()),newRouteList,getMessageId());
		
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
			
			Message toReturn = new ABreturnTokenMessage(getOriginalSenderInfo(),
					newReceiverInfo,getABSnapshotResult() , getSenderVectorClock(),Integer.parseInt(getMessageText()),getRoute(),getMessageId());
			
			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			
			return null;
		}
	}
	

	public ABSnapshotResult getABSnapshotResult() {
		return abSnapshotResult;
	}
	
	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}
	


}
