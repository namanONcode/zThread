# How Results Were Measured

## Profilers
We utilize the JMH built-in profilers to collect detailed telemetry during benchmark execution:
1. **GC Profiler (`-prof gc`)**: Measures allocation rates (MB/sec) and garbage collection pauses across implementations.
2. **Async Profiler (`-prof async`)**: (Linux only) Requires `perf_events`. Generates CPU and Allocation Flamegraphs to visualize hot paths and lock contention.
3. **Stack Profiler (`-prof stack`)**: Measures thread states and wait times, useful for profiling BlockingQueue lock contention versus lock-free event loop spinning.

## Modes
- **Throughput Mode (`Mode.Throughput`)**: Measures maximum raw operations per second. Used for scaling tests.
- **Sample Time (`Mode.SampleTime`)**: Measures execution latency at percentiles (P50, P90, P99, P99.9). Used for steady-state traffic measurement.
- **Single Shot (`Mode.SingleShotTime`)**: Used for burst traffic and idle runtime measurement.

## Result Aggregation
Results are serialized by JMH into `target/benchmark.json`. This JSON is then parsed by the Python charting script to generate SVG/PNG visualizations.
