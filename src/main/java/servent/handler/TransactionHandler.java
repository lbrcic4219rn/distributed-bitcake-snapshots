package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import lombok.RequiredArgsConstructor;
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

            int amountNumber;
            try {
                amountNumber = Integer.parseInt(amountString);
            } catch (NumberFormatException e) {
                AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
                return;
            }

            synchronized (CausalBroadcastShared.gatheringChannel) {
                bitcakeManager.addSomeBitcakes(amountNumber);
                if (bitcakeManager instanceof AcharyaBadrinathBitcakeManager abBitcakeManager) {
                    abBitcakeManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().id(), amountNumber);
                }
                if (bitcakeManager instanceof AlagarVenkatesanBitcakeManager avBitcakeManager) {
                    avBitcakeManager.recordGetTransaction(clientMessage.getSenderVectorClock(), clientMessage.getOriginalSenderInfo().id(), amountNumber);
                }
            }
        } else {
            AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
        }
    }
}
