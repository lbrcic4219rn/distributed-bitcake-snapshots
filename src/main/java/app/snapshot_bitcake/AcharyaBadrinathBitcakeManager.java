package app.snapshot_bitcake;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.Message;
import servent.message.snapshot.ABTellMessage;
import servent.message.snapshot.TokenMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AcharyaBadrinathBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    private final Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    public AcharyaBadrinathBitcakeManager() {
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            giveHistory.put(neighbor, 0);
            getHistory.put(neighbor, 0);
        }
    }

    @Override
    public void takeSomeBitcakes(int amount) {
        this.currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        this.currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return this.currentAmount.get();
    }

    public void sendTell(int tokenSenderId) {

        int myId = AppConfig.myServentInfo.id();
        Message tellMessage;
        synchronized (CausalBroadcastShared.gatheringChannel) {
            ABSnapshotResult snapshotResult = new ABSnapshotResult(myId, getCurrentBitcakeAmount(), giveHistory, getHistory);
            tellMessage = new ABTellMessage(AppConfig.myServentInfo, null, snapshotResult, tokenSenderId);
            CausalBroadcastShared.incrementClock(myId);
        }
        //No pause between sends - this runs while a committed TOKEN is being applied.
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            tellMessage = tellMessage.changeReceiver(neighbor);
            MessageUtil.sendMessage(tellMessage);
        }
    }

    public void sendToken(SnapshotCollector snapshotCollector) {

        int myId = AppConfig.myServentInfo.id();
        Message tokenMessage;
        ABSnapshotResult snapshotResult;
        synchronized (CausalBroadcastShared.gatheringChannel) {
            snapshotResult = new ABSnapshotResult(
                    myId, getCurrentBitcakeAmount(), giveHistory, getHistory);


            tokenMessage = new TokenMessage(
                    AppConfig.myServentInfo, null);
        }

        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            tokenMessage = tokenMessage.changeReceiver(neighbor);

            MessageUtil.sendMessage(tokenMessage);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Message toMe = new ABTellMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, snapshotResult, myId);
        CausalBroadcastShared.addPendingMessage(toMe);
        CausalBroadcastShared.checkPendingMessages(snapshotCollector);


    }

    public void recordGiveTransaction(int neighbor, int amount) {
        giveHistory.merge(neighbor, amount, Integer::sum);
    }

    public void recordGetTransaction(int neighbor, int amount) {
        getHistory.merge(neighbor, amount, Integer::sum);
    }
}
