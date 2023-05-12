package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;

	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();

			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			synchronized (CausalBroadcastShared.gatheringChannel) {
				bitcakeManager.addSomeBitcakes(amountNumber);
				if (bitcakeManager instanceof AcharyaBadrinathBitcakeManager) {
					AcharyaBadrinathBitcakeManager abBitcakeManager = (AcharyaBadrinathBitcakeManager) bitcakeManager;

					abBitcakeManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
				}
				if (bitcakeManager instanceof AlagarVenkatesanBitcakeManager) {
					AlagarVenkatesanBitcakeManager avBitcakeManager = (AlagarVenkatesanBitcakeManager) bitcakeManager;

					avBitcakeManager.recordGetTransaction(clientMessage.getSenderVectorClock(),clientMessage.getOriginalSenderInfo().getId(), amountNumber);
				}
			}


		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}


}
