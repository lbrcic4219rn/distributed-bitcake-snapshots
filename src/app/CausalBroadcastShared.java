package app;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiFunction;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.ABTellHandler;
import servent.handler.snapshot.DoneHandler;
import servent.handler.snapshot.TerminateHandler;
import servent.handler.snapshot.TokenHandler;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.snapshot.ABTellMessage;
import servent.message.snapshot.DoneMessage;
import servent.message.snapshot.TerminateMessage;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {

    private static Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static List<Message> commitedCausalMessageList = new CopyOnWriteArrayList<>();
    private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static Object pendingMessagesLock = new Object();
    public static Object gatheringChannel = new Object();
    private static ExecutorService commitedMessagesPoll = Executors.newCachedThreadPool();

    public static void initializeVectorClock(int serventCount) {
        for(int i = 0; i < serventCount; i++) {
            vectorClock.put(i, 0);
        }
    }

    public static boolean isEmptyPending(){return pendingMessages.isEmpty();}

    public static void incrementClock(int serventId) {
        vectorClock.computeIfPresent(serventId, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer key, Integer oldValue) {
                return oldValue+1;
            }
        });
    }

    public static Map<Integer, Integer> getVectorClock() {
        return vectorClock;
    }

    public static List<Message> getCommitedCausalMessages() {
        List<Message> toReturn = new CopyOnWriteArrayList<>(commitedCausalMessageList);

        return toReturn;
    }

    public static void addPendingMessage(Message msg) {
        pendingMessages.add(msg);
    }


    private static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for(int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

        return false;
    }

    public static boolean removeCommitedMessage(Message message){
        if(commitedCausalMessageList.remove(message))
            return true;
        return false;
    }

    public static void checkPendingMessages(SnapshotCollector snapshotCollector) {
        boolean gotWork = true;

        while (gotWork) {
            gotWork = false;

            synchronized (pendingMessagesLock) {
                Iterator<Message> iterator = pendingMessages.iterator();
                Map<Integer, Integer> myVectorClock = getVectorClock();
                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    //AppConfig.timestampedStandardPrint(myVectorClock);
//                    for (Map.Entry<Integer, Integer> entry1 : myVectorClock.entrySet()) {
//                        int key = entry1.getKey();
//                        int value1 = entry1.getValue();
//                        int value2 = pendingMessage.getSenderVectorClock().get(key);
//                        AppConfig.timestampedStandardPrint(value1+" - "+value2);
//                    }
                    //AppConfig.timestampedStandardPrint("\n");
                    if (!otherClockGreater(myVectorClock, pendingMessage.getSenderVectorClock())) {
                        gotWork = true;

                        switch (pendingMessage.getMessageType()) {
                            case TOKEN:
                                incrementClock(pendingMessage.getOriginalSenderInfo().getId());
                                commitedMessagesPoll.submit(new TokenHandler(pendingMessage,snapshotCollector.getBitcakeManager()));
                                break;
                            case AB_TELL:
                                incrementClock(pendingMessage.getOriginalSenderInfo().getId());
                                if(((ABTellMessage)pendingMessage).getInitiatorID()==AppConfig.myServentInfo.getId())
                                    commitedMessagesPoll.submit(new ABTellHandler(pendingMessage,snapshotCollector));
                                break;
                            case TRANSACTION:
                                incrementClock(pendingMessage.getOriginalSenderInfo().getId());
                                if(((TransactionMessage)pendingMessage).getIntendedReciver()==AppConfig.myServentInfo.getId())
                                    commitedMessagesPoll.submit(new TransactionHandler(pendingMessage,snapshotCollector.getBitcakeManager()));
                                break;
                            case DONE:
                                incrementClock(pendingMessage.getOriginalSenderInfo().getId());
                                if(((DoneMessage)pendingMessage).getInitiatorID()==AppConfig.myServentInfo.getId())
                                    commitedMessagesPoll.submit(new DoneHandler(pendingMessage,snapshotCollector));
                                break;
                            case TERMINATE:
                                incrementClock(pendingMessage.getOriginalSenderInfo().getId());
                                commitedMessagesPoll.submit(new TerminateHandler(pendingMessage,snapshotCollector));
                                break;
                        }

                        commitedCausalMessageList.add(pendingMessage);

                        iterator.remove();

                        break;
                    }
                }
            }
        }

    }
    public static void shutdown(){commitedMessagesPoll.shutdown();}
}
