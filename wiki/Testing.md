# Testing

zThread employs a multi-tiered testing strategy to ensure correctness in concurrent environments and prevent performance regressions.

## Unit Tests
Unit tests use **JUnit 5**, **Mockito**, and **AssertJ**.

They test isolated components, such as `ZRuntimeBuilder` validation, ring buffer logic, and basic event dispatcher behavior.

Run unit tests via:
```bash
./mvnw test --enable-native-access=ALL-UNNAMED
```

## Integration Tests
Integration tests (classes ending in `*IntegrationTest.java`) verify that the native Linux bindings interact correctly with the kernel. These tests allocate real `eventfd` and `epoll` instances and verify full round-trip event delivery across threads.

Run integration tests via the Failsafe plugin:
```bash
./mvnw verify --enable-native-access=ALL-UNNAMED
```

## Performance Benchmarks
To prevent performance regressions, the `zthread-benchmark` module contains a suite of **JMH** (Java Microbenchmark Harness) tests.

These benchmarks compare zThread against standard Java concurrent queues (`SynchronousQueue`), Netty's `EventLoop`, Project Reactor's `Schedulers`, and Vert.x.

### Running Benchmarks

```bash
# Build the benchmark executable
./mvnw clean package -pl zthread-benchmark -am

# Run a specific benchmark (e.g., SPSC event throughput)
java --enable-native-access=ALL-UNNAMED -jar zthread-benchmark/target/benchmarks.jar SpscEventBenchmark
```

### Chart Generation
The repository includes a Python script (`scripts/generate_charts.py`) that reads the `jmh-result.json` output and generates SVG radar charts to visualize performance across different concurrency scales. This script runs automatically in the CI pipeline.
