# Benchmark Methodology

This document outlines the strict methodology used to ensure fair and accurate comparisons between `zThread` and other concurrency primitives (such as Project Reactor, Netty, Vert.x, and Java BlockingQueues).

## The Principle of Identical Work
To prevent misleading benchmarks, every framework tested must perform exactly the same logical work. We enforce this through:
1. **Immutable Event Model**: `BenchmarkEvent` is a standard class used across all tests.
2. **Payload Parity**: Events carry an identical byte array payload (configurable via JMH `payloadSize`).
3. **Checksum Validation**: The `EventHandler` iterates through the payload and validates a checksum to simulate identical real-world processing work and prevent JIT dead-code elimination.
4. **Adapter Pattern**: Each framework is wrapped in an `EventRuntimeAdapter` to normalize start, submit, and shutdown behaviors.

## Benchmark Phases
- **Warmup**: Each benchmark undergoes multiple warmup iterations to allow the JVM to compile hot code paths and stabilize the JIT.
- **Measurement**: Data is collected over fixed-time iterations.
- **TearDown**: Runtimes are gracefully shut down between iterations to prevent resource leakage.

## JIT Compilation Fairness
By ensuring all frameworks invoke the identical `EventHandler#onEvent` logic, the JVM's JIT compiler optimizes the handler uniformly. We do not compare apples-to-oranges operations (e.g., raw queue insertion vs. full event loop dispatch); we compare the *complete event lifecycle* from submission to final processing.
