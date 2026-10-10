package servent.handler;

import lombok.RequiredArgsConstructor;
import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import servent.message.Message;
import servent.message.MessageType;

@RequiredArgsConstructor
public class TransactionHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	

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

					avBitcakeManager.recordGetTransaction(clientMessage.getSenderVectorClock(), clientMessage.getOriginalSenderInfo().getId(), amountNumber);
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}
}
