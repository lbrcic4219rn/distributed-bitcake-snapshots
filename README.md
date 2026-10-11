# Distributed bitcake snapshots

A small distributed system of nodes called servents. Each servent starts with 1000 bitcakes and sends random amounts to its neighbors. While the transfers are still under way, any servent can take a global snapshot and work out how many bitcakes exist in the whole system. With 7 servents the correct answer is always 7000.

Two snapshot algorithms are implemented, both on top of causal broadcast with vector clocks:

* Acharya and Badrinath (`snapshot=ab`)
* Alagar and Venkatesan (`snapshot=av`)

## Requirements

* JDK 21
* Nothing else. The Gradle wrapper downloads Gradle itself, and Lombok is fetched as a build dependency.

## Running

Each scenario lives in its own directory. Start one with:

```bash
./gradlew run --args=ab_snapshot
```

```bash
./gradlew run --args=av_snapshot
```

The starter launches every servent as a separate process, feeds each one its script, and waits for all of them to finish. A run takes about 50 seconds. If the servents do not stop by themselves, type `stop`.

## Reading the results

Every servent writes to its own files inside the scenario directory:

* `output/serventN_out.txt` for regular output
* `error/serventN_err.txt` for errors, which should stay empty

These files are rewritten on every run and are ignored by git.

For Acharya and Badrinath, the servent that started the snapshot prints the recorded amount of every servent, the amounts still traveling between servents, and the total:

```
Recorded bitcake amount for 0 = 948
Unreceived bitcake amount: 52 from servent 0 to servent 6
System bitcake count: 7000
```

For Alagar and Venkatesan, every servent prints its own part. The lines ending in `Total node bitcake amount` from all 7 servents add up to 7000 for each snapshot.

The `print_vector_clock` command also prints `Pending messages are empty: true` once every received message has been delivered.

## Scenario layout

```
ab_snapshot/
  servent_list.properties   the system: servent count, ports, neighbors, algorithm
  input/serventN_in.txt     the commands servent N runs, one per line
  output/                   written by the run
  error/                    written by the run
```

Example `servent_list.properties`:

```properties
servent_count=7
clique=false
fifo=false
snapshot=ab
servent0.port=1100
servent0.neighbors=1,2,6
```

When `clique` is false, every servent lists its neighbors and messages reach the rest of the system by rebroadcast.

## Commands

These can be used in the input scripts:

| Command | What it does |
|---|---|
| `info` | prints this servent and its neighbors |
| `pause N` | waits N milliseconds |
| `ping N` | pings neighbor N |
| `transaction_burst` | sends a burst of random transfers to every neighbor |
| `bitcake_info` | starts a snapshot from this servent |
| `print_vector_clock` | prints the vector clock and whether any messages are still pending |
| `stop` | stops the servent |

## How it works

* **Causal broadcast.** Every transaction and every snapshot message is broadcast to the whole system and carries the vector clock of its sender. A servent holds a message back until it has delivered everything the sender had seen when it sent it.
* **Clock stamped when sending.** A burst prepares all of its transfers up front and sends them over the next few seconds. The clock is written into a transfer at the moment it is sent, together with taking the bitcakes out of the sender, so a transfer sent after a snapshot is never mistaken for an earlier one.
* **Snapshot recorded on delivery.** When the snapshot token is delivered, the servent records its state in the same step that moves its clock forward. Everything delivered after that is applied in delivery order.
* **Reliable sending.** A sender waits for the receiver to close the connection and retries if the connection fails, because a single lost message would block everything that depends on it.

## Code layout

```
src/main/java/
  app/                     configuration, startup, causal broadcast state
  app/snapshot_bitcake/    the two algorithms and the snapshot collector
  cli/                     the command parser and the commands
  servent/                 the listener, message types, message handlers
```

Entry points are `app.MultipleServentStarter` (whole scenario) and `app.ServentMain` (one servent).
