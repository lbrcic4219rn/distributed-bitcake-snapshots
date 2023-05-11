package servent.message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABbitcakeManager;
import app.snapshot_bitcake.AVbitcakeManager;
import app.snapshot_bitcake.BitcakeManager;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;
	private Map<Integer, Integer> senderVectorClock;
	
	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount,BitcakeManager bitcakeManager,Map<Integer, Integer> senderVectorClock) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
		this.bitcakeManager = bitcakeManager;
		this.senderVectorClock = new ConcurrentHashMap<Integer, Integer>(senderVectorClock);
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		synchronized (AppConfig.colorLock) 
		{
			int amount = Integer.parseInt(getMessageText());
			
			bitcakeManager.takeSomeBitcakes(amount);
			if (bitcakeManager instanceof ABbitcakeManager) {
				ABbitcakeManager abFinancialManager = (ABbitcakeManager)bitcakeManager;
				
				abFinancialManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
			}
			if(bitcakeManager instanceof AVbitcakeManager)
			{
				AVbitcakeManager avFinancialManager = (AVbitcakeManager)bitcakeManager;
				
				avFinancialManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
			}
		}

	}
	
	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}
}
