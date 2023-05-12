package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class TerminateHandler implements MessageHandler {

    private Message clientMessage;
    private BitcakeManager bitcakeManager;
    private SnapshotCollector snapshotCollector;

    public TerminateHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = snapshotCollector.getBitcakeManager();
        this.snapshotCollector = snapshotCollector;
    }


    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.TERMINATE) {
                ((AlagarVenkatesanBitcakeManager)bitcakeManager).recieveTerminate(snapshotCollector);
            } else {
                AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
