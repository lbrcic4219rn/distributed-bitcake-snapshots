package cli.command;

import lombok.RequiredArgsConstructor;
import java.util.List;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.util.FifoSendWorker;

@RequiredArgsConstructor
public class StopCommand implements CLICommand {

	private final CLIParser parser;
	private final SimpleServentListener listener;
	private final List<FifoSendWorker> senderWorkers;
	private final SnapshotCollector snapshotCollector;
	
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		for (FifoSendWorker senderWorker : senderWorkers) {
			senderWorker.stop();
		}
		snapshotCollector.stop();
		CausalBroadcastShared.shutdown();
	}
}
