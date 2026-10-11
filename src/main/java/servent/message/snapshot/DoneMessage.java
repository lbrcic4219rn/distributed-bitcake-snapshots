package servent.message.snapshot;

import java.io.Serial;
import app.ServentInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import servent.message.BasicMessage;
import servent.message.MessageType;

@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DoneMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 2390820142409006505L;
    @Getter
    private int initiatorID;

    public DoneMessage(ServentInfo sender, ServentInfo receiver, int initiatorID) {
        super(MessageType.DONE, sender, receiver);
        this.initiatorID = initiatorID;
    }

}
