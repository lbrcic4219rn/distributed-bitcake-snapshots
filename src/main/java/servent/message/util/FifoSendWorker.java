package servent.message.util;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PoisonMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * We will have as many instances of these workers as we have neighbors. Each of them
 * reads messages from a queue (two queues, actually, for Chandy-Lamport) and sends them
 * via a simple socket. The thread waits for an ACK on the same socket before sending another
 * message to the same servent.
 * <p>
 * These threads are stopped via {@link PoisonMessage}.
 *
 * @author bmilojkovic
 *
 */
public class FifoSendWorker implements Runnable, Cancellable {

    private final int neighbor;

    public FifoSendWorker(int neighbor) {
        this.neighbor = neighbor;
    }

    @Override
    public void run() {
        boolean running = true;

        while (running) {
            try {
                Message messageToSend = nextMessage();

                if (messageToSend != null) {
                    if (messageToSend.getMessageType() == MessageType.POISON) {
                        running = false;
                    } else {
                        send(messageToSend);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (IOException | ClassNotFoundException e) {
                AppConfig.timestampedErrorPrint("Couldn't send FIFO message to " + neighbor + " (" + e + ")");
            }
        }
    }

    /**
     * Markers go first. Regular messages are only taken while we are white.
     */
    private Message nextMessage() throws InterruptedException {
        Message messageToSend = MessageUtil.getPendingMarkers(neighbor).poll(200, TimeUnit.MILLISECONDS);

        if (messageToSend == null && AppConfig.isWhite.get()) {
            messageToSend = MessageUtil.getPendingMessages(neighbor).poll(200, TimeUnit.MILLISECONDS);
        }

        return messageToSend;
    }

    private void send(Message messageToSend) throws IOException, ClassNotFoundException {
        ServentInfo receiverInfo = messageToSend.getReceiverInfo();

        try (Socket sendSocket = new Socket(receiverInfo.ipAddress(), receiverInfo.listenerPort())) {
            /*
             * We are doing a compound operation on our color. In Chandy-Lamport, we want to be sure
             * that we are white when we are taking away our bitcakes.
             * The bitcake reducing is done in the sendEffect() method of the
             * Transaction message, which is at the bottom of this sync.
             */
            synchronized (AppConfig.colorLock) {

                if (MessageUtil.MESSAGE_UTIL_PRINTING) {
                    AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
                }

                ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
                oos.writeObject(messageToSend);
                oos.flush();

                messageToSend.sendEffect();
            }

            ObjectInputStream ois = new ObjectInputStream(sendSocket.getInputStream());
            String ackString = (String) ois.readObject();
            if (!ackString.equals("ACK")) {
                AppConfig.timestampedErrorPrint("Got response which is not an ACK");
            }
        }
    }

    @Override
    public void stop() {
        try {
            MessageUtil.getPendingMessages(neighbor).put(new PoisonMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
