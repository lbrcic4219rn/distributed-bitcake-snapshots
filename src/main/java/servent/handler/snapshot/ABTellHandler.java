package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import lombok.RequiredArgsConstructor;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ABTellMessage;

@RequiredArgsConstructor
public class ABTellHandler implements MessageHandler {
    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.AB_TELL) {
                ABTellMessage abTellMessage = (ABTellMessage) clientMessage;
                snapshotCollector.addABSnapshotInfo(
                        abTellMessage.getOriginalSenderInfo().id(),
                        abTellMessage.getAbSnapshotResult()
                );
            } else {
                AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("ABTellHandler failed (" + e + ")");
        }
    }
}
