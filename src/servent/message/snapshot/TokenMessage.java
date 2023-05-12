package servent.message.snapshot;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenMessage extends BasicMessage {


    private static final long serialVersionUID = -2718696242684977713L;

    public TokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(MessageType.TOKEN, originalSenderInfo, receiverInfo);
    }

    public TokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, List<ServentInfo> routeList, String messageText, int messageId, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.TOKEN, originalSenderInfo, receiverInfo, routeList, messageText, messageId,senderVectorClock);
    }
    @Override
    public Message changeReceiver(Integer newReceiverId) {
        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            Message toReturn = new TokenMessage(getOriginalSenderInfo(),
                    newReceiverInfo, getRoute(), getMessageText(), getMessageId(),getSenderVectorClock());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

            return null;
        }

    }
    @Override
    public Message makeMeASender() {
        ServentInfo newRouteItem = AppConfig.myServentInfo;

        List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
        newRouteList.add(newRouteItem);
        Message toReturn = new TokenMessage(getOriginalSenderInfo(),
                getReceiverInfo(), newRouteList, getMessageText(), getMessageId(),getSenderVectorClock());

        return toReturn;
    }
}
