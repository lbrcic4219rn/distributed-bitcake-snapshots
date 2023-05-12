package app.snapshot_bitcake;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.Message;
import servent.message.snapshot.DoneMessage;
import servent.message.snapshot.TerminateMessage;
import servent.message.snapshot.TokenMessage;
import servent.message.util.MessageUtil;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class AlagarVenkatesanBitcakeManager implements BitcakeManager{
    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    private int recordedAmount;

    private int initiatorId;

    private Map<Integer, Integer> tokenVectorClock = null;

    public void takeSomeBitcakes(int amount) { currentAmount.getAndAdd(-amount);}

    public void addSomeBitcakes(int amount) { currentAmount.getAndAdd(amount);}

    public int getCurrentBitcakeAmount() { return currentAmount.get();}

    private Map<Integer, Integer> getChannel = new ConcurrentHashMap<>();
    private Map<Integer, Integer> giveChannel = new ConcurrentHashMap<>();

    public AlagarVenkatesanBitcakeManager() {

    }

    public void sendTerminate(SnapshotCollector snapshotCollector) {

        int myId = AppConfig.myServentInfo.getId();
        Message terminateMessage=null;
        synchronized (CausalBroadcastShared.gatheringChannel) {
            terminateMessage = new TerminateMessage(AppConfig.myServentInfo, null);


            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                terminateMessage = terminateMessage.changeReceiver(neighbor);

                MessageUtil.sendMessage(terminateMessage);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        CausalBroadcastShared.addPendingMessage(terminateMessage);
        CausalBroadcastShared.checkPendingMessages(snapshotCollector);
    }

    public void sendToken() {
        Message tokenMessage=null;
        initiatorId = AppConfig.myServentInfo.getId();
        synchronized (CausalBroadcastShared.gatheringChannel) {
            for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                getChannel.put(neighbor, 0);
                giveChannel.put(neighbor, 0);
            }
          recordedAmount =getCurrentBitcakeAmount();
          tokenMessage = new TokenMessage(AppConfig.myServentInfo, null);
          tokenVectorClock = tokenMessage.getSenderVectorClock();

            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                tokenMessage = tokenMessage.changeReceiver(neighbor);
                MessageUtil.sendMessage(tokenMessage);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            CausalBroadcastShared.incrementClock(initiatorId);
        }

    }

    public void recieveToken(Map<Integer, Integer> tokenVectorClock, int initiatorId){
        Message doneMessage=null;
        synchronized (CausalBroadcastShared.gatheringChannel) {
            for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                getChannel.put(neighbor, 0);
                giveChannel.put(neighbor, 0);
            }
            this.tokenVectorClock = tokenVectorClock;
            this.recordedAmount = getCurrentBitcakeAmount();
            this.initiatorId = initiatorId;
            doneMessage = new DoneMessage(AppConfig.myServentInfo,null,initiatorId);

            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                doneMessage = doneMessage.changeReceiver(neighbor);
                MessageUtil.sendMessage(doneMessage);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());
        }

    }

    public void recieveTerminate(SnapshotCollector snapshotCollector){
        synchronized (CausalBroadcastShared.gatheringChannel) {
            this.tokenVectorClock = null;
        }
        snapshotCollector.setTerminateNotArrived();
        int sum = recordedAmount;
        AppConfig.timestampedStandardPrint("Recorded bitcake amount: "+recordedAmount);
        for (Map.Entry<Integer, Integer> entry : getChannel.entrySet()) {
            AppConfig.timestampedStandardPrint("Unreceived bitcake amount: "+ entry.getValue() +" from "+ entry.getKey());
            sum+=entry.getValue();
        }
        for (Map.Entry<Integer, Integer> entry : giveChannel.entrySet()) {
            AppConfig.timestampedStandardPrint("Sent bitcake amount: "+ entry.getValue() +" from "+ entry.getKey());
            sum-=entry.getValue();
        }
        AppConfig.timestampedStandardPrint("Total node bitcake amount: "+sum+"\n");
    }

    private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {

        private int valueToAdd;

        public MapValueUpdater(int valueToAdd) {
            this.valueToAdd = valueToAdd;
        }

        @Override
        public Integer apply(Integer key, Integer oldValue) {
            return oldValue + valueToAdd;
        }
    }

    public void recordGetTransaction(Map<Integer, Integer> senderVectorClock,int neighbor, int amount) {
        if(tokenVectorClock!=null){
            if(senderVectorClock.get(initiatorId)<=tokenVectorClock.get(initiatorId))
                getChannel.compute(neighbor, new AlagarVenkatesanBitcakeManager.MapValueUpdater(amount));
        }
    }
    public void recordGiveTransaction(Map<Integer, Integer> senderVectorClock,int neighbor, int amount) {
        if(tokenVectorClock!=null){
            if(senderVectorClock.get(initiatorId)<=tokenVectorClock.get(initiatorId))
                giveChannel.compute(neighbor, new AlagarVenkatesanBitcakeManager.MapValueUpdater(amount));
        }
    }

    public int getRecordedAmount() {
        return recordedAmount;
    }

    public Map<Integer, Integer> getTokenVectorClock() {
        return tokenVectorClock;
    }
}
