package servent.message;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.AcharyaBadrinathBitcakeManager;
import app.snapshot_bitcake.AlagarVenkatesanBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;


/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 *
 * @author bmilojkovic
 *
 */
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TransactionMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -333251402058492901L;

    private transient BitcakeManager bitcakeManager;
    @Getter
    private int intendedReceiver;

    public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager) {
        super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
        this.bitcakeManager = bitcakeManager;
        this.intendedReceiver = receiver.id();
    }

    /**
     * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
     * This method is invoked by the sender just before sending.
     * <p>
     * The vector clock is stamped here, and not when the message was made. A burst makes all of
     * its messages up front and sends them over the next few seconds - with the old stamp a
     * transaction sent after our snapshot still looked like it was sent before it.
     */
    @Override
    public void sendEffect() {
        if (bitcakeManager != null) {
            int amount = Integer.parseInt(getMessageText());

            synchronized (CausalBroadcastShared.gatheringChannel) {
                getSenderVectorClock().putAll(CausalBroadcastShared.getVectorClock());
                bitcakeManager.takeSomeBitcakes(amount);
                CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.id());

                if (bitcakeManager instanceof AcharyaBadrinathBitcakeManager acharyaBadrinathBitcakeManager) {
                    acharyaBadrinathBitcakeManager.recordGiveTransaction(getReceiverInfo().id(), amount);
                }
                if (bitcakeManager instanceof AlagarVenkatesanBitcakeManager alagarVenkatesanBitcakeManager) {
                    alagarVenkatesanBitcakeManager.recordGiveTransaction(getSenderVectorClock(), getReceiverInfo().id(), amount);
                }
            }
        }
    }

}
