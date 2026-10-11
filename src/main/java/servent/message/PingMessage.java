package servent.message;

import java.io.Serial;
import app.ServentInfo;

public class PingMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -1934709147043909111L;

    public PingMessage(ServentInfo senderInfo, ServentInfo receiverInfo) {
        super(MessageType.PING, senderInfo, receiverInfo, "PING");

    }

}
