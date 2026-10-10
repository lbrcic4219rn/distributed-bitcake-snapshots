package servent.message.util;

import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or somesuch.
 * 
 * @author bmilojkovic
 *
 */
@RequiredArgsConstructor
public class DelayedMessageSender implements Runnable {

	private static final int MAX_SEND_ATTEMPTS = 20;
	private static final int CLOSE_WAIT_MS = 5000;

	private final Message messageToSend;
	
	
	public void run() {
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 1000) + 500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		ServentInfo receiverInfo = messageToSend.getReceiverInfo();
		
		if (MessageUtil.MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
		}
		
		/*
		 * Causal broadcast needs reliable channels - a dropped message leaves everything
		 * that depends on it stuck in the pending queue. During bursts the receiver's
		 * accept queue overflows and the connect fails, so we back off and try again.
		 */
		IOException lastError = null;
		for (int attempt = 0; attempt < MAX_SEND_ATTEMPTS; attempt++) {
			try {
				/*
				 * Similar sync block to the one in FifoSenderWorker, except this one is
				 * related to Lai-Yang. We want to be sure that message color is red if we
				 * are red. Just setting the attribute when we were making the message may
				 * have been to early.
				 * All messages that declare their own stuff (eg. LYTellMessage) will have
				 * to override setRedColor() because of this.
				 */
				synchronized (AppConfig.colorLock) {

					Socket sendSocket = new Socket(receiverInfo.getIpAddress(), receiverInfo.getListenerPort());

					ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
					oos.writeObject(messageToSend);
					oos.flush();

					/*
					 * Wait for the receiver to close its end first. If we close first, our
					 * ephemeral port sits in TIME_WAIT, and a transaction burst runs the
					 * machine out of ports ("Can't assign requested address").
					 */
					try {
						sendSocket.setSoTimeout(CLOSE_WAIT_MS);
						sendSocket.getInputStream().read();
					} catch (IOException e) {
						//The message is already written. Nothing more to do here.
					}

					sendSocket.close();

					messageToSend.sendEffect();
				}
				return;
			} catch (IOException e) {
				lastError = e;
			}

			try {
				Thread.sleep((long)(Math.random() * 400) + 100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString() + " (" + lastError + ")");
	}
	
}
