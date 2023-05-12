package servent.message;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;
	private int intendedReciver;
	
	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
		this.bitcakeManager = bitcakeManager;
		this.intendedReciver = receiver.getId();
	}

	private TransactionMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
						  List<ServentInfo> routeList, String messageText, int messageId,  Map<Integer, Integer> senderVectorClock,BitcakeManager bitcakeManager, int intendedReciver) {
		super(MessageType.TRANSACTION,originalSenderInfo,receiverInfo,routeList, messageText,messageId,senderVectorClock);
		this.intendedReciver = intendedReciver;
		this.bitcakeManager = bitcakeManager;

	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			Message toReturn = new TransactionMessage(getOriginalSenderInfo(),
					newReceiverInfo, getRoute(), getMessageText(), getMessageId(),getSenderVectorClock(),bitcakeManager,intendedReciver);

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
		Message toReturn = new TransactionMessage(getOriginalSenderInfo(),
				getReceiverInfo(), newRouteList, getMessageText(), getMessageId(),getSenderVectorClock(),bitcakeManager,intendedReciver);

		return toReturn;
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		if(bitcakeManager!=null) {
			int amount = Integer.parseInt(getMessageText());

			synchronized (CausalBroadcastShared.gatheringChannel){
			bitcakeManager.takeSomeBitcakes(amount);

			CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());

			if (bitcakeManager instanceof AcharyaBadrinathBitcakeManager) {
				AcharyaBadrinathBitcakeManager abFinancialManager = (AcharyaBadrinathBitcakeManager) bitcakeManager;

				abFinancialManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
			}
				if (bitcakeManager instanceof AlagarVenkatesanBitcakeManager) {
					AlagarVenkatesanBitcakeManager avFinancialManager = (AlagarVenkatesanBitcakeManager) bitcakeManager;

					avFinancialManager.recordGiveTransaction(getSenderVectorClock(),getReceiverInfo().getId(),amount);
				}
			}
		}

	}

	public int getIntendedReciver() {
		return intendedReciver;
	}
}
