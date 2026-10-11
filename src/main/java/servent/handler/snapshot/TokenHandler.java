package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import lombok.RequiredArgsConstructor;
import servent.handler.MessageHandler;
import servent.message.Message;

@RequiredArgsConstructor
public class TokenHandler implements MessageHandler {
    private final Message clientMessage;
    private final BitcakeManager bitcakeManager;

    @Override
    public void run() {
        try {
            if (bitcakeManager instanceof AcharyaBadrinathBitcakeManager abBitcakeManager)
                abBitcakeManager.sendTell(clientMessage.getOriginalSenderInfo().id());
            if (bitcakeManager instanceof AlagarVenkatesanBitcakeManager avBitcakeManager)
                avBitcakeManager.receiveToken(clientMessage.getSenderVectorClock(), clientMessage.getOriginalSenderInfo().id());
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("TokenHandler failed (" + e + ")");
        }
    }
}
