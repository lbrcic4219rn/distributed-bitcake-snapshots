package app;

import lombok.Getter;
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

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {
    public static final Object gatheringChannel = new Object();
    @Getter
    private static final Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static final Object pendingMessagesLock = new Object();
    /*
     * One thread, so handlers run in the same order the messages were committed in.
     * With a pool, a transaction committed right after a TOKEN could be applied before
     * the snapshot state was recorded, and its bitcakes were counted twice.
     */
    private static final ExecutorService commitedMessagesPoll = Executors.newSingleThreadExecutor();

    private CausalBroadcastShared() {
        //Static shared state only.
    }

    public static void initializeVectorClock(int serventCount) {
        for (int i = 0; i < serventCount; i++) {
            vectorClock.put(i, 0);
        }
    }

    public static boolean isEmptyPending() {
        return pendingMessages.isEmpty();
    }

    public static void incrementClock(int serventId) {
        vectorClock.computeIfPresent(serventId, (key, oldValue) -> oldValue + 1);
    }

    public static void addPendingMessage(Message msg) {
        pendingMessages.add(msg);
    }

    private static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for (int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

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

                    if (!otherClockGreater(myVectorClock, pendingMessage.getSenderVectorClock())) {
                        gotWork = true;

                        deliver(pendingMessage, snapshotCollector);

                        iterator.remove();

                        break;
                    }
                }
            }
        }

    }

    /**
     * Commits one message: moves our clock forward for its sender and hands it to its handler.
     */
    private static void deliver(Message pendingMessage, SnapshotCollector snapshotCollector) {
        int myId = AppConfig.myServentInfo.id();
        int senderId = pendingMessage.getOriginalSenderInfo().id();

        switch (pendingMessage.getMessageType()) {
            case TOKEN:
                /*
                 * The snapshot state is recorded right here, together with the clock change.
                 * Everything we send from now on is stamped as sent after the snapshot,
                 * so nothing may be sent between the two.
                 */
                synchronized (gatheringChannel) {
                    incrementClock(senderId);
                    new TokenHandler(pendingMessage, snapshotCollector.getBitcakeManager()).run();
                }
                break;
            case AB_TELL:
                incrementClock(senderId);
                if (((ABTellMessage) pendingMessage).getInitiatorID() == myId)
                    commitedMessagesPoll.submit(new ABTellHandler(pendingMessage, snapshotCollector));
                break;
            case TRANSACTION:
                incrementClock(senderId);
                if (((TransactionMessage) pendingMessage).getIntendedReceiver() == myId)
                    commitedMessagesPoll.submit(new TransactionHandler(pendingMessage, snapshotCollector.getBitcakeManager()));
                break;
            case DONE:
                incrementClock(senderId);
                if (((DoneMessage) pendingMessage).getInitiatorID() == myId)
                    commitedMessagesPoll.submit(new DoneHandler(pendingMessage, snapshotCollector));
                break;
            case TERMINATE:
                incrementClock(senderId);
                commitedMessagesPoll.submit(new TerminateHandler(pendingMessage, snapshotCollector));
                break;
            default:
                //Other message types are not causally ordered.
                break;
        }
    }

    public static void shutdown() {
        commitedMessagesPoll.shutdown();
    }
}
