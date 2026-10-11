package app;

import app.snapshot_bitcake.SnapshotType;
import lombok.extern.java.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

/**
 * This class contains all the global application configuration stuff.
 *
 * @author bmilojkovic
 *
 */
@Log
public class AppConfig {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final List<ServentInfo> serventInfoList = new ArrayList<>();
    /**
     * Convenience access for this servent's information
     */
    public static ServentInfo myServentInfo;
    /**
     * If this is true, the system is a clique - all nodes are each other's
     * neighbors.
     */
    public static boolean IS_CLIQUE;
    /**
     * If this is true, messages will be send purely via FIFO -
     * if message M1 is sent before message M2 from one node to another,
     * M1 will also be received before M2.
     */
    public static boolean IS_FIFO;
    /**
     * Snapshot algorithm selection. Possible values are:
     * <ul>
     * <li>naive - No algorithm</li>
     * <li>cl - Chandy-Lamport - works on FIFO systems</li>
     * <li>ly - Lai-Yang - works on non-FIFO systems</li>
     * </ul>
     */
    public static SnapshotType SNAPSHOT_TYPE;
    public static AtomicBoolean isWhite = new AtomicBoolean(true);
    public static final Object colorLock = new Object();

    /*
     * Regular messages go to stdout and errors to stderr, both as "HH:mm:ss - message".
     * MultipleServentStarter points those two streams at the output and error files.
     */
    static {
        log.setUseParentHandlers(false);
        log.addHandler(streamHandler(FileDescriptor.out, logRecord -> logRecord.getLevel().intValue() < Level.SEVERE.intValue()));
        log.addHandler(streamHandler(FileDescriptor.err, logRecord -> logRecord.getLevel().intValue() >= Level.SEVERE.intValue()));
    }

    private AppConfig() {
        //Static configuration only.
    }

    private static Handler streamHandler(FileDescriptor stream, Filter filter) {
        Formatter timestamped = new Formatter() {
            @Override
            public String format(LogRecord logRecord) {
                return TIME_FORMAT.format(logRecord.getInstant()) + " - " + logRecord.getMessage() + System.lineSeparator();
            }
        };

        Handler handler = new StreamHandler(new FileOutputStream(stream), timestamped) {
            @Override
            public synchronized void publish(LogRecord logRecord) {
                super.publish(logRecord);
                flush();
            }
        };
        handler.setFilter(filter);

        return handler;
    }

    /**
     * Print a message to stdout with a timestamp
     *
     * @param message message to print
     */
    public static void timestampedStandardPrint(String message) {
        log.info(message);
    }

    /**
     * Print a message to stderr with a timestamp
     *
     * @param message message to print
     */
    public static void timestampedErrorPrint(String message) {
        log.severe(message);
    }

    /**
     * Reads a config file. Should be called once at start of app.
     * The config file should be of the following format:
     * <br/>
     * <code><br/>
     * servent_count=3 			- number of servents in the system <br/>
     * clique=false 			- is it a clique or not <br/>
     * fifo=false				- should sending be fifo
     * servent0.port=1100 		- listener ports for each servent <br/>
     * servent1.port=1200 <br/>
     * servent2.port=1300 <br/>
     * servent0.neighbors=1,2 	- if not a clique, who are the neighbors <br/>
     * servent1.neighbors=0 <br/>
     * servent2.neighbors=0 <br/>
     *
     * </code>
     * <br/>
     * So in this case, we would have three servents, listening on ports:
     * 1100, 1200, and 1300. This is not a clique, and:<br/>
     * servent 0 sees servent 1 and 2<br/>
     * servent 1 sees servent 0<br/>
     * servent 2 sees servent 0<br/>
     *
     * @param configName name of configuration file
     */
    public static void readConfig(String configName) {
        Properties properties = new Properties();
        try (FileInputStream configStream = new FileInputStream(configName)) {
            properties.load(configStream);
        } catch (IOException e) {
            timestampedErrorPrint("Couldn't open properties file. Exiting...");
            System.exit(0);
        }

        int serventCount = readIntOrExit(properties, "servent_count");

        IS_CLIQUE = Boolean.parseBoolean(properties.getProperty("clique", "false"));
        IS_FIFO = Boolean.parseBoolean(properties.getProperty("fifo", "false"));
        CausalBroadcastShared.initializeVectorClock(serventCount);
        SNAPSHOT_TYPE = readSnapshotType(properties);

        for (int i = 0; i < serventCount; i++) {
            int serventPort = readIntOrExit(properties, "servent" + i + ".port");

            ServentInfo newInfo = new ServentInfo("localhost", i, serventPort, readNeighbors(properties, i, serventCount));
            serventInfoList.add(newInfo);
        }
    }

    private static int readIntOrExit(Properties properties, String propertyName) {
        try {
            return Integer.parseInt(properties.getProperty(propertyName));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading " + propertyName + ". Exiting...");
            System.exit(0);
            return -1;
        }
    }

    private static SnapshotType readSnapshotType(Properties properties) {
        return switch (properties.getProperty("snapshot", "none")) {
            case "ab" -> SnapshotType.ACHARYA_BADRINATH;
            case "av" -> SnapshotType.ALAGAR_VENKATESAN;
            default -> {
                timestampedErrorPrint("Problem reading snapshot algorithm. Defaulting to NONE.");
                yield SnapshotType.NONE;
            }
        };
    }

    private static List<Integer> readNeighbors(Properties properties, int serventId, int serventCount) {
        List<Integer> neighborList = new ArrayList<>();

        if (IS_CLIQUE) {
            for (int j = 0; j < serventCount; j++) {
                if (j != serventId) {
                    neighborList.add(j);
                }
            }
            return neighborList;
        }

        String neighborListProp = properties.getProperty("servent" + serventId + ".neighbors");

        if (neighborListProp == null) {
            timestampedErrorPrint("Warning: graph is not clique, and node " + serventId + " doesnt have neighbors");
            return neighborList;
        }

        try {
            for (String neighbor : neighborListProp.split(",")) {
                neighborList.add(Integer.parseInt(neighbor));
            }
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Bad neighbor list for node " + serventId + ": " + neighborListProp);
        }

        return neighborList;
    }

    /**
     * Get info for a servent selected by a given id.
     *
     * @param id id of servent to get info for
     * @return {@link ServentInfo} object for this id
     */
    public static ServentInfo getInfoById(int id) {
        if (id >= getServentCount()) {
            throw new IllegalArgumentException(
                    "Trying to get info for servent " + id + " when there are " + getServentCount() + " servents.");
        }
        return serventInfoList.get(id);
    }

    /**
     * Get number of servents in this system.
     */
    public static int getServentCount() {
        return serventInfoList.size();
    }

}
