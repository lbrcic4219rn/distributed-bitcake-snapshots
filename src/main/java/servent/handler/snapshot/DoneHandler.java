package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import lombok.RequiredArgsConstructor;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

@RequiredArgsConstructor
public class DoneHandler implements MessageHandler {
    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.DONE) {
                snapshotCollector.addDoneMessage(clientMessage.getOriginalSenderInfo().id());
            } else {
                AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("DoneHandler failed (" + e + ")");
        }
    }
}
