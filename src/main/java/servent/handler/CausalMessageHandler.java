package servent.handler;

import lombok.RequiredArgsConstructor;
import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class CausalMessageHandler implements MessageHandler {

    private final Message clientMessage;
    private final boolean doRebroadcast;
    private final SnapshotCollector snapshotCollector;
    private static Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<Message, Boolean>());

    @Override
    public void run() {
        try {
            ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
            if(doRebroadcast) {
                if(senderInfo.getId() == AppConfig.myServentInfo.getId()) {
                    AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
                    return;
                }

                boolean didPut = receivedBroadcasts.add(clientMessage);

                if(didPut) {
                    CausalBroadcastShared.addPendingMessage(clientMessage);
                    CausalBroadcastShared.checkPendingMessages(snapshotCollector);
                    AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
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
            e.printStackTrace();
        }
    }
}
