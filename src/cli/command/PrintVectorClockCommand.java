package cli.command;

import app.AppConfig;
import app.CausalBroadcastShared;



import java.util.HashMap;
import java.util.Map;

public class PrintVectorClockCommand implements CLICommand {



    @Override
    public String commandName() {
        return "print_vector_clock";
    }

    @Override
    public void execute(String args) {
        Map<Integer,Integer> vectorclock = new HashMap<>(CausalBroadcastShared.getVectorClock());
        for (Map.Entry<Integer, Integer> entry1 : vectorclock.entrySet()) {
            AppConfig.timestampedStandardPrint(String.valueOf(entry1.getValue()));
        }
       AppConfig.timestampedStandardPrint("Pending messages are empty: "+ CausalBroadcastShared.isEmptyPending());

    }

}
