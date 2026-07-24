# zThread Benchmark Suite

This module contains an enterprise-grade JMH benchmark suite to validate the performance, scalability, and efficiency of **zThread** against industry-standard runtimes and data structures.

## Scenarios

The suite measures end-to-end event pipeline performance (Submit -> Wake -> Dispatch -> Handle -> Complete) rather than isolated syscalls.

### 1. Queues and Event Dispatch
* `SpscEventBenchmark`: 1 Producer, 1 Consumer. Evaluates raw queue capacity vs event loops.
* `MpscEventBenchmark`: 4 Producers, 1 Consumer. Evaluates contention on event submission.
* `MpmcEventBenchmark`: 16 Producers, 4 Consumers. Evaluates highly concurrent multi-producer, multi-consumer event loop groups.

### 2. Runtime Metrics
* `IdleRuntimeBenchmark`: Measures baseline CPU utilization, context switches, and power consumption of an idle event loop. Validates the "near-zero CPU" claim.

### 3. Subsystems
* `TimerExecutionBenchmark`: Evaluates Timer scheduling and execution precision.
* `LoopbackTcpBenchmark`: Evaluates Socket dispatch latency over loopback.
* `FileWatchBenchmark`: Evaluates File system event latency (inotify vs NIO WatchService).
* `SignalBenchmark`: Evaluates POSIX signal handling latency.

## Technologies Compared
* **Standard Java**: `ArrayBlockingQueue`, `LinkedBlockingQueue`, `ConcurrentLinkedQueue`, `SynchronousQueue`, `ScheduledExecutorService`, `WatchService`.
* **Virtual Threads**: Java 21+ Virtual Thread per Task Executors.
* **Reactive Frameworks**: Project Reactor (`Sinks`, `Schedulers`).
* **Event Loop Frameworks**: Netty (EventLoop, HashedWheelTimer), Eclipse Vert.x (EventBus).
* **zThread**: The Linux-native kernel event loop.

## Running the Benchmarks

To run the entire suite:

```bash
cd scripts/
./run-all.sh
```

To run only the Linux-specific benchmarks (requires Linux):
```bash
./run-linux.sh
```

To profile a specific benchmark with `async-profiler` and JMH `-prof gc`:
```bash
./profile.sh SpscEventBenchmark
```

To generate a performance report (requires Python 3 and matplotlib):
```bash
./generate-report.sh
```

## Configuration
Make sure you are running on a Linux machine with Java 21+ and have FFM API access enabled (`--enable-native-access=ALL-UNNAMED`). ZGC is recommended for consistent latency (`-XX:+UseZGC`).
