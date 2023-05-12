package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;


public class TokenHandler implements MessageHandler {

    private Message clientMessage;
    private BitcakeManager bitcakeManager;

    public TokenHandler(Message clientMessage, BitcakeManager bitcakeManager) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        try{
            if(bitcakeManager instanceof AcharyaBadrinathBitcakeManager)
                ((AcharyaBadrinathBitcakeManager)bitcakeManager).sendTell(clientMessage.getOriginalSenderInfo().getId());
            if(bitcakeManager instanceof AlagarVenkatesanBitcakeManager)
                ((AlagarVenkatesanBitcakeManager)bitcakeManager).recieveToken(clientMessage.getSenderVectorClock(),clientMessage.getOriginalSenderInfo().getId());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
