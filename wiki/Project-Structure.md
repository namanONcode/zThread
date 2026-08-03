# Project Structure

The zThread repository is a multi-module Maven project. The codebase is organized by responsibility to isolate native Linux dependencies from core Java interfaces and reactive bindings.

## Modules

### `zthread-core`
Contains all OS-agnostic interfaces and core abstractions.
* **Purpose:** Defines the public API contract for the runtime.
* **Important files:** 
  * `ZRuntime.java`: The main interface for posting events and managing the loop.
  * `ZRuntimeBuilder.java`: Fluent builder for creating a runtime.
  * `EventDispatcher.java`: Interface for registering event handlers.

### `zthread-linux`
The native Linux implementation of the core interfaces.
* **Purpose:** Implements the event loop using `epoll` via the Foreign Function & Memory (FFM) API.
* **Important files:**
  * `LinuxRuntime.java`: The concrete implementation of `ZRuntime`.
  * `LinuxEventLoop.java`: The thread that runs the `epoll_wait` loop.
  * `LinuxNativePoller.java`: The FFM bridge calling into `libc.so.6`.

### `zthread-reactor`
Provides bindings to Project Reactor.
* **Purpose:** Allows bridging zThread events into reactive `Flux` and `Mono` streams.
* **Important files:**
  * `ZEventFlux.java`: Converts the event loop dispatch mechanism into a reactive publisher.

### `zthread-benchmark`
Contains the JMH performance benchmarks.
* **Purpose:** Validates throughput and latency against other frameworks (Netty, Vert.x, standard queues).
* **Important files:**
  * `SpscEventBenchmark.java`: Tests single-producer single-consumer throughput.
  * `S1_S5_ThroughputLatencyBenchmark.java`: Tests various concurrency scaling scenarios.

### `zthread-examples`
Provides executable examples of how to use the library.

## Build and Tooling Directories
* `.github/workflows`: Contains CI/CD pipelines for building, benchmarking, and Dependabot auto-merging.
* `scripts`: Contains Python scripts (e.g., `generate_charts.py`) used to parse JMH JSON output into the SVG graphs displayed in the README.
* `config`: Contains XML rulesets for static analysis tools (Checkstyle, PMD, SpotBugs).
