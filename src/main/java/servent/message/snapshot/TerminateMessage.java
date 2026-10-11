package servent.message.snapshot;

import app.ServentInfo;
import lombok.experimental.SuperBuilder;
import servent.message.BasicMessage;
import servent.message.MessageType;

@SuperBuilder(toBuilder = true)
public class TerminateMessage extends BasicMessage {
    public TerminateMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(MessageType.TERMINATE, originalSenderInfo, receiverInfo);
    }
}
