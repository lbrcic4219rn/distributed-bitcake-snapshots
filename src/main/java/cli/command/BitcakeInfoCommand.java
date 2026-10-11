package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BitcakeInfoCommand implements CLICommand {

    private final SnapshotCollector collector;


    @Override
    public String commandName() {
        return "bitcake_info";
    }

    @Override
    public void execute(String args) {
        if (!collector.isCollecting())
            collector.startCollecting();
        else
            AppConfig.timestampedErrorPrint("Already collecting");
    }

}
