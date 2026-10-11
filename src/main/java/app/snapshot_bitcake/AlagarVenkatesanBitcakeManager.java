package app.snapshot_bitcake;

import app.AppConfig;
import app.CausalBroadcastShared;
import lombok.Getter;
import servent.message.Message;
import servent.message.snapshot.DoneMessage;
import servent.message.snapshot.TerminateMessage;
import servent.message.snapshot.TokenMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AlagarVenkatesanBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    @Getter
    private int recordedAmount;
    private int initiatorId;
    @Getter
    private Map<Integer, Integer> tokenVectorClock = null;
    private final Map<Integer, Integer> getChannel = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> giveChannel = new ConcurrentHashMap<>();

    @Override
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    /**
     * Sends the message to every neighbor. Sending is asynchronous, so this doesn't block -
     * it is called from the thread that applies committed messages, which must not be held up.
     *
     * @return the copy that went to the last neighbor
     */
    private Message sendToNeighbors(Message message) {
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            message = message.changeReceiver(neighbor);
            MessageUtil.sendMessage(message);
        }

        return message;
    }

    public void sendTerminate(SnapshotCollector snapshotCollector) {
        Message terminateMessage;
        synchronized (CausalBroadcastShared.gatheringChannel) {
            terminateMessage = new TerminateMessage(AppConfig.myServentInfo, null);
        }

        terminateMessage = sendToNeighbors(terminateMessage);

        CausalBroadcastShared.addPendingMessage(terminateMessage);
        CausalBroadcastShared.checkPendingMessages(snapshotCollector);
    }

    public void sendToken() {
        Message tokenMessage;
        initiatorId = AppConfig.myServentInfo.id();
        synchronized (CausalBroadcastShared.gatheringChannel) {
            for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                getChannel.put(neighbor, 0);
                giveChannel.put(neighbor, 0);
            }
            recordedAmount = getCurrentBitcakeAmount();
            tokenMessage = new TokenMessage(AppConfig.myServentInfo, null);
            tokenVectorClock = tokenMessage.getSenderVectorClock();
            CausalBroadcastShared.incrementClock(initiatorId);
        }

        sendToNeighbors(tokenMessage);
    }

    public void receiveToken(Map<Integer, Integer> tokenVectorClock, int initiatorId) {
        Message doneMessage;
        synchronized (CausalBroadcastShared.gatheringChannel) {
            for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                getChannel.put(neighbor, 0);
                giveChannel.put(neighbor, 0);
            }
            this.tokenVectorClock = tokenVectorClock;
            this.recordedAmount = getCurrentBitcakeAmount();
            this.initiatorId = initiatorId;
            doneMessage = new DoneMessage(AppConfig.myServentInfo, null, initiatorId);
            CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.id());
        }

        sendToNeighbors(doneMessage);
    }

    public void receiveTerminate(SnapshotCollector snapshotCollector) {
        synchronized (CausalBroadcastShared.gatheringChannel) {
            this.tokenVectorClock = null;
        }
        snapshotCollector.setTerminateNotArrived();
        int sum = recordedAmount;
        AppConfig.timestampedStandardPrint("Recorded bitcake amount: " + recordedAmount);
        for (Map.Entry<Integer, Integer> entry : getChannel.entrySet()) {
            AppConfig.timestampedStandardPrint("Unreceived bitcake amount: " + entry.getValue() + " from " + entry.getKey());
            sum += entry.getValue();
        }
        for (Map.Entry<Integer, Integer> entry : giveChannel.entrySet()) {
            AppConfig.timestampedStandardPrint("Sent bitcake amount: " + entry.getValue() + " from " + entry.getKey());
            sum -= entry.getValue();
        }
        AppConfig.timestampedStandardPrint("Total node bitcake amount: " + sum + "\n");
    }

    public void recordGetTransaction(Map<Integer, Integer> senderVectorClock, int neighbor, int amount) {
        if (tokenVectorClock != null && senderVectorClock.get(initiatorId) <= tokenVectorClock.get(initiatorId)) {
            getChannel.merge(neighbor, amount, Integer::sum);
        }
    }

    public void recordGiveTransaction(Map<Integer, Integer> senderVectorClock, int neighbor, int amount) {
        if (tokenVectorClock != null && senderVectorClock.get(initiatorId) <= tokenVectorClock.get(initiatorId)) {
            giveChannel.merge(neighbor, amount, Integer::sum);
        }
    }

}
