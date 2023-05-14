package servent.message.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class ABTellMessage extends BasicMessage {

	private static final long serialVersionUID = 1536279421038991652L;
	private ABSnapshotResult abSnapshotResult;
	private int initiatorID;
	
	public ABTellMessage(ServentInfo sender, ServentInfo receiver, ABSnapshotResult abSnapshotResult, int initiatorID) {
		super(MessageType.AB_TELL, sender, receiver);
		
		this.abSnapshotResult = abSnapshotResult;
		this.initiatorID = initiatorID;
	}
	
	private ABTellMessage(ServentInfo sender, ServentInfo receiver, List<ServentInfo> routeList, String messageText, int messageId,
						  ABSnapshotResult abSnapshotResult, Map<Integer, Integer> senderVectorClock, int initiatorID) {
		super(MessageType.AB_TELL, sender, receiver, routeList, messageText, messageId, senderVectorClock);
		this.abSnapshotResult = abSnapshotResult;
		this.initiatorID = initiatorID;
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			Message toReturn = new ABTellMessage(getOriginalSenderInfo(),
					newReceiverInfo, getRoute(), getMessageText(), getMessageId(),abSnapshotResult,getSenderVectorClock(),initiatorID);

			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}

	}

	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;

		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);
		Message toReturn = new ABTellMessage(getOriginalSenderInfo(),
				getReceiverInfo(), newRouteList, getMessageText(), getMessageId(),abSnapshotResult,getSenderVectorClock(),initiatorID);

		return toReturn;
	}

	public ABSnapshotResult getAbSnapshotResult() {
		return abSnapshotResult;
	}

	public int getInitiatorID() {
		return initiatorID;
	}
}
