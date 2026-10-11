package servent.message;

import java.io.Serial;
import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A default message implementation. This should cover most situations.
 * Subclasses that add fields need <code>@SuperBuilder(toBuilder = true)</code>, so the
 * modificator methods (changeReceiver, makeMeASender) copy them too.
 *
 * @author bmilojkovic
 *
 */
@Getter
@SuperBuilder(toBuilder = true)
public class BasicMessage implements Message {

    @Serial
    private static final long serialVersionUID = -9075856313609777945L;
    //This gives us a unique id - incremented in every natural constructor.
    private static AtomicInteger messageCounter = new AtomicInteger(0);
    private final MessageType messageType;
    private final ServentInfo originalSenderInfo;
    private final ServentInfo receiverInfo;
    private final List<ServentInfo> route;
    private final String messageText;
    private final Map<Integer, Integer> senderVectorClock;
    private final int messageId;

    public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        this.messageType = type;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.senderVectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());
        this.route = new ArrayList<>();
        this.messageText = "";

        this.messageId = messageCounter.getAndIncrement();
    }

    public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                        String messageText) {
        this.messageType = type;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.senderVectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());
        this.route = new ArrayList<>();
        this.messageText = messageText;

        this.messageId = messageCounter.getAndIncrement();
    }


    /**
     * Used when resending a message. It will not change the original owner
     * (so equality is not affected), but will add us to the route list, so
     * message path can be retraced later.
     */
    @Override
    public Message makeMeASender() {
        ServentInfo newRouteItem = AppConfig.myServentInfo;

        List<ServentInfo> newRouteList = new ArrayList<>(route);
        newRouteList.add(newRouteItem);
        return toBuilder().route(newRouteList).build();
    }

    /**
     * Change the message received based on ID. The receiver has to be our neighbor.
     * Use this when you want to send a message to multiple neighbors, or when resending.
     */
    @Override
    public Message changeReceiver(Integer newReceiverId) {
        if (AppConfig.myServentInfo.neighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            return toBuilder().receiverInfo(newReceiverInfo).build();
        } else {
            AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

            return null;
        }

    }

    /**
     * Comparing messages is based on their unique id and the original sender id.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof BasicMessage other
                && getMessageId() == other.getMessageId()
                && getOriginalSenderInfo().id() == other.getOriginalSenderInfo().id();
    }

    /**
     * Hash needs to mirror equals, especially if we are gonna keep this object
     * in a set or a map. So, this is based on message id and original sender id also.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getMessageId(), getOriginalSenderInfo().id());
    }

    /**
     * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
     */
    @Override
    public String toString() {
        return "[" + getOriginalSenderInfo().id() + "|" + getMessageId() + "|" +
                getMessageText() + "|" + getMessageType() + "|" +
                getReceiverInfo().id() + "]";
    }

    /**
     * Empty implementation, which will be suitable for most messages.
     */
    @Override
    public void sendEffect() {
        //Most messages have nothing to do when they are sent.
    }

}
