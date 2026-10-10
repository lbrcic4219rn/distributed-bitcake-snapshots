package servent.handler.snapshot;

import lombok.RequiredArgsConstructor;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;

@RequiredArgsConstructor
public class TokenHandler implements MessageHandler {
    private final Message clientMessage;
    private final BitcakeManager bitcakeManager;

    @Override
    public void run() {
        try{
            if(bitcakeManager instanceof AcharyaBadrinathBitcakeManager)
                ((AcharyaBadrinathBitcakeManager)bitcakeManager).sendTell(clientMessage.getOriginalSenderInfo().getId());
            if(bitcakeManager instanceof AlagarVenkatesanBitcakeManager)
                ((AlagarVenkatesanBitcakeManager)bitcakeManager).receiveToken(clientMessage.getSenderVectorClock(), clientMessage.getOriginalSenderInfo().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
