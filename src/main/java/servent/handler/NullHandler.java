package servent.handler;

import app.AppConfig;
import lombok.RequiredArgsConstructor;
import servent.message.Message;

/**
 * This will be used if no proper handler is found for the message.
 *
 * @author bmilojkovic
 *
 */
@RequiredArgsConstructor
public class NullHandler implements MessageHandler {

    private final Message clientMessage;


    @Override
    public void run() {
        AppConfig.timestampedErrorPrint("Couldn't handle message: " + clientMessage);
    }

}
