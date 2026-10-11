package app;

import java.io.Serializable;
import java.util.List;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public record ServentInfo(int id, String ipAddress, int listenerPort, List<Integer> neighbors) implements Serializable {

    public ServentInfo(String ipAddress, int id, int listenerPort, List<Integer> neighbors) {
        this(id, ipAddress, listenerPort, neighbors);
    }


    @Override
    public String toString() {
        return "[" + id + "|" + ipAddress + "|" + listenerPort + "]";
    }
}
