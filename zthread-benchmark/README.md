# zThread Benchmark Suite

This module contains a production-grade, statistically valid JMH benchmark suite designed to evaluate the performance, scalability, and efficiency of **zThread** against industry-standard runtimes and concurrency primitives.

## Principle of Fair Comparison

To prevent misleading microbenchmarks, every framework tested performs **exactly the same logical work**.
- **Immutable Event Model**: `BenchmarkEvent` is a standard class used across all tests.
- **Payload Parity**: Events carry an identical byte array payload (configurable via JMH `payloadSize`: 64B, 256B, 1KB, 4KB).
- **Checksum Validation**: The `EventHandler` iterates through the payload and validates a checksum to simulate identical real-world processing work and prevent JIT dead-code elimination.
- **Adapter Pattern**: Each framework is wrapped in an `EventRuntimeAdapter` to normalize start, submit, and shutdown behaviors.

See `docs/Benchmark Methodology.md` and `docs/Fair Comparison.md` for more details.

## Benchmark Scenarios

The suite maps to 14 distinct scenarios to test different aspects of event loops and concurrency.

### Core Event Lifecycle (Scenarios 1-5)
The `S1_S5_ThroughputLatencyBenchmark` measures raw throughput across various producer-consumer (P:C) concurrency levels:
- **Scenario 1**: 1 Producer, 1 Consumer (1:1)
- **Scenario 2**: 4 Producers, 1 Consumer (4:1)
- **Scenario 3**: 8 Producers, 1 Consumer (8:1)
- **Scenario 4**: 16 Producers, 4 Consumers (16:4)
- **Scenario 5**: 32 Producers, 8 Consumers (32:8)

### Behavior Scenarios
- **Scenario 6 (Idle Runtime)**: `S6_IdleRuntimeBenchmark` measures CPU usage and context switches of idle runtimes.
- **Scenario 7 (Burst Traffic)**: `S7_BurstTrafficBenchmark` submits 1,000,000 events as fast as possible and measures the completion time (SingleShotTime).
- **Scenario 8 (Steady Traffic)**: `S8_SteadyTrafficBenchmark` uses fixed-rate throttling to measure latency percentiles (P99) under steady load (1000/s, 5000/s).

### Subsystems (Scenarios 10-14)
- **Scenario 10 (Scheduler Benchmark)**: `S10_SchedulerBenchmark` evaluates Timer scheduling overhead against Netty `HashedWheelTimer` and `ScheduledExecutorService`.
- **Scenario 11 (Socket Benchmark)**: `LoopbackTcpBenchmark` evaluates Socket dispatch latency over loopback.
- **Scenario 12 (File Watch Benchmark)**: `FileWatchBenchmark` evaluates File system event latency (inotify vs NIO WatchService).
- **Scenario 13 (Signal Benchmark)**: `SignalBenchmark` evaluates POSIX signal handling latency.
- **Scenario 14 (Custom Event Benchmark)**: Handled comprehensively via Scenarios 1-8.

## Technologies Compared
* **Standard Java**: `ArrayBlockingQueue`, `LinkedBlockingQueue`, `ConcurrentLinkedQueue` (via TransferQueue), `SynchronousQueue`, `ScheduledExecutorService`, `ForkJoinPool`.
* **Virtual Threads**: Java 21+ Virtual Thread per Task Executors.
* **Reactive Frameworks**: Project Reactor (`Sinks.Many`, `Schedulers`).
* **Event Loop Frameworks**: Netty (`EpollEventLoopGroup`), Eclipse Vert.x (`EventBus`).
* **zThread**: The Linux-native kernel event loop.

## Running the Benchmarks

To execute the `zThread` benchmark suite locally and reproduce the official results:

```bash
# 1. Compile the suite
./mvnw clean compile

# 2. Run Benchmarks (outputs JSON and CSV to target/benchmark.json)
./mvnw exec:exec -pl zthread-benchmark -Dexec.executable="java" -Dexec.args="-classpath %classpath org.openjdk.jmh.Main -rf json -rff target/benchmark.json"

# 3. Generate Charts (outputs to charts/ and benchmark.md)
cd zthread-benchmark
python3 scripts/generate_charts.py target/benchmark.json
```

**Note on Profiling:** To collect OS-level context switches and CPU Flamegraphs via `async-profiler` (using the JMH `-prof async` or `-prof gc` flags), you must lower the `perf_event_paranoid` level on Linux:
```bash
sudo sysctl -w kernel.perf_event_paranoid=1
sudo sysctl -w kernel.kptr_restrict=0
```

## Documentation

For a deep dive into the methodology, hardware, and analysis, please refer to the `docs/` folder:
- [Benchmark Methodology](docs/Benchmark%20Methodology.md)
- [How Results Were Measured](docs/How%20Results%20Were%20Measured.md)
- [Fair Comparison](docs/Fair%20Comparison.md)
- [Reproducing Results](docs/Reproducing%20Results.md)
- [Hardware Used](docs/Hardware%20Used.md)

## Continuous Integration (CI)

This benchmark suite is integrated into GitHub Actions via `.github/workflows/benchmark.yml`.

The CI workflow can be triggered on-demand and automatically performs the following:
1. Provisions an isolated Linux runner and configures `perf_events` for deep hardware profiling.
2. Compiles the `zThread` project and the benchmark suite.
3. Executes the full JMH suite across all 11 frameworks, generating raw JSON metrics.
4. Invokes the `generate_charts.py` script to generate high-resolution SVG/PNG charts (e.g., `scaling_throughput.png`).
5. Generates an aggregated Markdown report (`benchmark.md`).
6. Uploads the raw JSON, generated charts, and final report as a downloadable GitHub Artifact (`benchmark-results`).

You can download the charts directly from the Actions tab on GitHub to inspect the latest benchmark results!
