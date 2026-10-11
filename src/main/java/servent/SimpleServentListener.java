package servent;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.SnapshotCollector;
import lombok.RequiredArgsConstructor;
import servent.handler.*;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class SimpleServentListener implements Runnable, Cancellable {

    private final SnapshotCollector snapshotCollector;
    /*
     * Thread pool for executing the handlers. Each client will get it's own handler thread.
     */
    private final ExecutorService threadPool = Executors.newWorkStealingPool();
    private volatile boolean working = true;

    @Override
    public void run() {
        try (ServerSocket listenerSocket = new ServerSocket(AppConfig.myServentInfo.listenerPort(), 100)) {
            /*
             * If there is no connection after 1s, wake up and see if we should terminate.
             */
            listenerSocket.setSoTimeout(1000);

            while (working) {
                acceptMessage(listenerSocket);
            }
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.listenerPort());
            System.exit(0);
        }
    }

    private void acceptMessage(ServerSocket listenerSocket) {
        try {

            /*
             * This blocks for up to 1s, after which SocketTimeoutException is thrown.
             */
            Socket clientSocket = listenerSocket.accept();

            //GOT A MESSAGE! <3
            Message clientMessage = MessageUtil.readMessage(clientSocket);

            MessageHandler messageHandler = new NullHandler(clientMessage);

            /*
             * Each message type has it's own handler.
             * If we can get away with stateless handlers, we will,
             * because that way is much simpler and less error prone.
             */
            switch (clientMessage.getMessageType()) {
                case PING:
                    messageHandler = new PingHandler(clientMessage);
                    break;
                case PONG:
                    messageHandler = new PongHandler(clientMessage);
                    break;
                case POISON:
                    break;
                default:
                    messageHandler = new CausalMessageHandler(clientMessage, !AppConfig.IS_CLIQUE, snapshotCollector);
                    break;
            }

            threadPool.submit(messageHandler);
        } catch (SocketTimeoutException timeoutEx) {
            //Nothing arrived in the last second. Go around and check if we should still be working.
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Problem accepting a connection (" + e + ")");
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
