package app;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import app.snapshot_bitcake.ABbitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.snapshot.ab.ReturnTokenH;
import servent.handler.snapshot.ab.TokenH;
import servent.handler.snapshot.ab.TransactionH;
import servent.handler.snapshot.av.AVDoneH;
import servent.handler.snapshot.av.AVTerminateH;
import servent.handler.snapshot.av.AVTokenH;
import servent.message.ClockUpdateMessage;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.snapshot.ABreturnTokenMessage;
import servent.message.snapshot.ABtokenMessage;
import servent.message.snapshot.AVTerminateMessage;
import servent.message.snapshot.AVdoneMessage;
import servent.message.snapshot.AVtokenMessage;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 * 
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {

	private static Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
	private static List<Message> commitedCausalMessageList = new CopyOnWriteArrayList<>();
	private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
	private static Object pendingMessagesLock = new Object();
	public static SnapshotCollector snapshotCollector;
	public static final ExecutorService handlerThreadPool = Executors.newCachedThreadPool();
	
	public static void intializeSnapshotCollector(SnapshotCollector collector)
	{
		snapshotCollector= collector;
	}
	
	public static void initializeVectorClock(int serventCount) {
		for(int i = 0; i < serventCount; i++) {
			vectorClock.put(i, 0);
		}
	}
	
	public static void incrementClock(int serventId) {
		vectorClock.computeIfPresent(serventId, new BiFunction<Integer, Integer, Integer>() {

			@Override
			public Integer apply(Integer key, Integer oldValue) {
				return oldValue+1;
			}
		});
	}
	
	public static Map<Integer, Integer> getVectorClock() {
		return vectorClock;
	}
	
	public static List<Message> getCommitedCausalMessages() {
		List<Message> toReturn = new CopyOnWriteArrayList<>(commitedCausalMessageList);
		
		return toReturn;
	}
	
	public static void addPendingMessage(Message msg) {
		pendingMessages.add(msg);
	}
	
	public static Queue<Message> getPendingMessages() {
		return pendingMessages;
	}
	
	public static void commitCausalMessage(Message newMessage) {
		AppConfig.timestampedStandardPrint("Committing " + newMessage);
		commitedCausalMessageList.add(newMessage);
		incrementClock(newMessage.getOriginalSenderInfo().getId());
		
		checkPendingMessages();
	}
	
	private static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
		if (clock1.size() != clock2.size()) {
			throw new IllegalArgumentException("Clocks are not same size how why");
		}
		
		for(int i = 0; i < clock1.size(); i++) {
			if (clock2.get(i) > clock1.get(i)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static void transactionHandle(Message newMessage)
	{
		String amountString = newMessage.getMessageText();
		
		int amountNumber = 0;
		try {
			amountNumber = Integer.parseInt(amountString);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
			return;
		}
		
		
		snapshotCollector.getBitcakeManager().addSomeBitcakes(amountNumber);

		synchronized (AppConfig.colorLock) 
		{
			if (snapshotCollector.getBitcakeManager() instanceof ABbitcakeManager) 
			{
				ABbitcakeManager abBitcakeManager = (ABbitcakeManager)snapshotCollector.getBitcakeManager();
				
				abBitcakeManager.recordGetTransaction(newMessage.getOriginalSenderInfo().getId(), amountNumber);
			}
		}
		
	}
	
	public static void tokenHandle(Message newMessage)
	{
		ABbitcakeManager abBitcakeManaget = (ABbitcakeManager)snapshotCollector.getBitcakeManager();
		abBitcakeManaget.tokenEvent(Integer.parseInt(newMessage.getMessageText()), snapshotCollector);
	}
	
	public static void returnTokenHandle(Message newMessage)
	{
		if(AppConfig.myServentInfo.getId() == Integer.parseInt(newMessage.getMessageText()))
		{
			ABreturnTokenMessage abReturnTokenMessage = (ABreturnTokenMessage)newMessage;
			snapshotCollector.addABSnapshotInfo(abReturnTokenMessage.getOriginalSenderInfo().getId(),
												abReturnTokenMessage.getABSnapshotResult());
		}
	}
	
	
	public static void checkPendingMessages() {
		boolean gotWork = true;
		
		while (gotWork) {
			gotWork = false;
			
			synchronized (pendingMessagesLock) {
				Iterator<Message> iterator = pendingMessages.iterator();
				
				Map<Integer, Integer> myVectorClock = getVectorClock();
				while (iterator.hasNext()) {
					Message pendingMessage = iterator.next();
					if(pendingMessage instanceof TransactionMessage)
					{
						TransactionMessage transactionPendingMessage = (TransactionMessage)pendingMessage;
						
						if (!otherClockGreater(myVectorClock, transactionPendingMessage.getSenderVectorClock())) {
							gotWork = true;
							
//							transactionHandle(pendingMessage);
							handlerThreadPool.submit(new TransactionH(pendingMessage));
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
					}
					if(pendingMessage instanceof ABtokenMessage)
					{
						ABtokenMessage abTokenPendingMessage = (ABtokenMessage)pendingMessage;
						
						
						if (!otherClockGreater(myVectorClock, abTokenPendingMessage.getSenderVectorClock())) {
							gotWork = true;
							
//							tokenHandle(pendingMessage);
							handlerThreadPool.submit(new TokenH(pendingMessage));
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
					}
					if(pendingMessage instanceof ABreturnTokenMessage)
					{
						ABreturnTokenMessage abreturnTokenPendingMessage = (ABreturnTokenMessage)pendingMessage;
						
						
						if (!otherClockGreater(myVectorClock, abreturnTokenPendingMessage.getSenderVectorClock())) {
							gotWork = true;
							
							returnTokenHandle(pendingMessage);
							handlerThreadPool.submit(new ReturnTokenH(pendingMessage));
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
					}
					if(pendingMessage instanceof ClockUpdateMessage)
					{
						ClockUpdateMessage clockUpdateMessage = (ClockUpdateMessage)pendingMessage;
						
						
						if (!otherClockGreater(myVectorClock, clockUpdateMessage.getSenderVectorClock())) {
							gotWork = true;
							
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
						
					}
										/*Poruke za AV algoritam*/
					
					if(pendingMessage instanceof AVdoneMessage)
					{
						AVdoneMessage avDoneMessage = (AVdoneMessage)pendingMessage;
						
						
						if (!otherClockGreater(myVectorClock, avDoneMessage.getSenderVectorClock())) {
							gotWork = true;
							handlerThreadPool.submit(new AVDoneH(pendingMessage));
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
						
					}
					
					if(pendingMessage instanceof AVtokenMessage)
					{
						AVtokenMessage avTokenMessage = (AVtokenMessage)pendingMessage;
						
						
						if (!otherClockGreater(myVectorClock, avTokenMessage.getSenderVectorClock())) {
							gotWork = true;
							handlerThreadPool.submit(new AVTokenH(pendingMessage));
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
						
					}
					
					if(pendingMessage instanceof AVTerminateMessage)
					{
						AVTerminateMessage avTerminateMessage = (AVTerminateMessage)pendingMessage;
						
						handlerThreadPool.submit(new AVTerminateH());
						if (!otherClockGreater(myVectorClock, avTerminateMessage.getSenderVectorClock())) {
							gotWork = true;
							
							incrementClock(pendingMessage.getOriginalSenderInfo().getId());
		
							iterator.remove();
							
							break;
						}
						
					}
					
					
				}
			}
		}
		
	}
}
