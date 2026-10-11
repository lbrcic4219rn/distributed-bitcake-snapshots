package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ABTellMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1536279421038991652L;
    private ABSnapshotResult abSnapshotResult;
    private int initiatorID;

    public ABTellMessage(ServentInfo sender, ServentInfo receiver, ABSnapshotResult abSnapshotResult, int initiatorID) {
        super(MessageType.AB_TELL, sender, receiver);

        this.abSnapshotResult = abSnapshotResult;
        this.initiatorID = initiatorID;
    }

}
