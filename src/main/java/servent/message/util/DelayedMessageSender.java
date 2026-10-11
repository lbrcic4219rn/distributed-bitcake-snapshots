package servent.message.util;

import app.AppConfig;
import app.ServentInfo;
import lombok.RequiredArgsConstructor;
import servent.message.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

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
        sleep(ThreadLocalRandom.current().nextLong(500, 1500));

        ServentInfo receiverInfo = messageToSend.getReceiverInfo();

        if (MessageUtil.MESSAGE_UTIL_PRINTING) {
            AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
        }

        /*
         * Done once, before the message is on the wire - the receiver must never
         * see a transaction that we haven't taken out of our own state yet.
         */
        messageToSend.sendEffect();

        /*
         * Causal broadcast needs reliable channels - a dropped message leaves everything
         * that depends on it stuck in the pending queue. During bursts the receiver's
         * accept queue overflows and the connect fails, so we back off and try again.
         */
        IOException lastError = null;
        for (int attempt = 0; attempt < MAX_SEND_ATTEMPTS; attempt++) {
            try {
                send(receiverInfo);
                return;
            } catch (IOException e) {
                lastError = e;
            }

            sleep(ThreadLocalRandom.current().nextLong(100, 500));
        }

        AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend + " (" + lastError + ")");
    }

    private void send(ServentInfo receiverInfo) throws IOException {
        /*
         * Similar sync block to the one in FifoSenderWorker, except this one is
         * related to Lai-Yang. We want to be sure that message color is red if we
         * are red. Just setting the attribute when we were making the message may
         * have been too early.
         * All messages that declare their own stuff (e.g. LYTellMessage) will have
         * to override setRedColor() because of this.
         */
        synchronized (AppConfig.colorLock) {
            try (Socket sendSocket = new Socket(receiverInfo.ipAddress(), receiverInfo.listenerPort())) {
                ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
                oos.writeObject(messageToSend);
                oos.flush();

                waitForReceiverClose(sendSocket);
            }
        }
    }

    /**
     * Wait for the receiver to close its end first. If we close first, our
     * ephemeral port sits in TIME_WAIT, and a transaction burst runs the
     * machine out of ports ("Can't assign requested address").
     */
    private void waitForReceiverClose(Socket sendSocket) {
        try {
            sendSocket.setSoTimeout(CLOSE_WAIT_MS);
            InputStream fromReceiver = sendSocket.getInputStream();
            while (fromReceiver.read() != -1) {
                //The receiver sends nothing back. We are only waiting for it to close.
            }
        } catch (IOException e) {
            //The message is already written. Nothing more to do here.
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
