package servent.message.snapshot;

import java.io.Serial;
import app.ServentInfo;
import lombok.experimental.SuperBuilder;
import servent.message.BasicMessage;
import servent.message.MessageType;

@SuperBuilder(toBuilder = true)
public class TokenMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -2718696242684977713L;

    public TokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(MessageType.TOKEN, originalSenderInfo, receiverInfo);
    }
}
