package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotCollector;
import lombok.RequiredArgsConstructor;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class CausalMessageHandler implements MessageHandler {

    private static final Set<Message> receivedBroadcasts = ConcurrentHashMap.newKeySet();
    private final Message clientMessage;
    private final boolean doRebroadcast;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        try {
            ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
            if (doRebroadcast) {
                if (senderInfo.id() == AppConfig.myServentInfo.id()) {
                    AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
                    return;
                }

                boolean didPut = receivedBroadcasts.add(clientMessage);

                if (didPut) {
                    CausalBroadcastShared.addPendingMessage(clientMessage);
                    CausalBroadcastShared.checkPendingMessages(snapshotCollector);
                    AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

                    for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                        //Same message, different receiver, and add us to the route table.
                        MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
                    }
                } else {
                    AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
                }
            } else {
                CausalBroadcastShared.addPendingMessage(clientMessage);
                CausalBroadcastShared.checkPendingMessages(snapshotCollector);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("CausalMessageHandler failed (" + e + ")");
        }
    }
}
