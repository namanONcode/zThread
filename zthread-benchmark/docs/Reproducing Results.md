# Reproducing Results

To execute the `zThread` benchmark suite locally and reproduce the official results:

## Prerequisites
1. JDK 24 or 25 installed.
2. Maven 3.9+
3. Python 3.10+ (with `matplotlib`, `pandas`, `seaborn`) for chart generation.
4. **Linux Only**: To collect OS-level context switches and CPU Flamegraphs via `async-profiler`, you must lower the `perf_event_paranoid` level:
   ```bash
   sudo sysctl -w kernel.perf_event_paranoid=1
   sudo sysctl -w kernel.kptr_restrict=0
   ```

## Execution
Run the complete suite using Maven. This will clean, verify, and run the JMH benchmarks using the `exec-maven-plugin`.

```bash
# 1. Compile the suite
./mvnw clean compile

# 2. Run Benchmarks (outputs JSON and CSV to zthread-benchmark/target/)
./mvnw exec:exec -pl zthread-benchmark -Dexec.executable="java" -Dexec.args="-classpath %classpath org.openjdk.jmh.Main -rf json -rff target/benchmark.json"

# 3. Generate Charts (outputs to zthread-benchmark/charts/)
cd zthread-benchmark
python3 scripts/generate_charts.py target/benchmark.json
```

All charts and the markdown report will be generated automatically in the `zthread-benchmark/charts/` directory.
