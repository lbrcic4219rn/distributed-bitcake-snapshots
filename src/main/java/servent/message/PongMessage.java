package servent.message;

import app.ServentInfo;

import java.io.Serial;

public class PongMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -71866183898007085L;

    public PongMessage(ServentInfo senderInfo, ServentInfo receiverInfo) {
        super(MessageType.PONG, senderInfo, receiverInfo, "PONG");
    }

}
