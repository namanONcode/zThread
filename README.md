<p align="center">
  <img src="banner.png" alt="zThread Banner" width="100%">
</p>

# zThread

[![CI](https://github.com/namanONcode/zThread/actions/workflows/ci.yml/badge.svg)](https://github.com/namanONcode/zThread/actions/workflows/ci.yml)
[![Java 25](https://img.shields.io/badge/Java-25-blue.svg?logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Linux Native](https://img.shields.io/badge/Linux-Native-FCC624?logo=linux&logoColor=black)]()
[![Maven Central](https://img.shields.io/maven-central/v/io.github.namanoncode/zthread-core.svg)](https://central.sonatype.com/artifact/io.github.namanoncode/zthread-core)
[![License](https://img.shields.io/github/license/namanONcode/zThread)](LICENSE)

> Millions of events. Near-zero idle CPU. One event loop.

A Linux-native event runtime for Java that sleeps inside `epoll_wait()` instead of busy-spinning, enabling millions of events with near-zero idle CPU.

## The Problem

Traditional Java servers often rely on:
* Thread-per-connection
* Busy polling
* High idle CPU
* Context switching

zThread uses Linux `epoll` directly so your application sleeps until the kernel has actual work.

## Hello World

```java
ZRuntime runtime = ZRuntime.builder().build();

runtime.on(SocketEvent.class, event -> {
    System.out.println("Connected");
});

runtime.start();
```

## Architecture

```mermaid
graph TD
    Client -->|Network| LinuxKernel[Linux Kernel]
    LinuxKernel -->|epoll_wait| EventLoop[zThread Event Loop]
    EventLoop --> Dispatcher
    Dispatcher --> JavaHandler[Java Handler]
    JavaHandler --> Application
```

## Performance

*(Benchmarks executed with JMH on Linux, JDK 25. Graphs generated automatically via GitHub Actions CI)*

### Core Throughput & Latency
<p align="center">
  <img src="assets/benchmarks/throughput_chart.svg" width="48%" alt="Throughput Benchmark">
  <img src="assets/benchmarks/latency_distribution.svg" width="48%" alt="Latency Benchmark">
</p>

### Scaling Matrix & Idle Efficiency
<p align="center">
  <img src="assets/benchmarks/scaling_matrix.svg" width="48%" alt="Scaling Benchmark">
  <img src="assets/benchmarks/idle_cpu.svg" width="48%" alt="Idle CPU Benchmark">
</p>

### Benchmark Breakdown & Latency Matrix

<!-- BENCHMARK_TABLE_START -->
| Framework / Mechanism | Throughput (Higher is better) | Average Latency (Lower is better) | Engine Architecture |
| :--- | :--- | :--- | :--- |
| **zThread (Linux FFM / Epoll)** | **~9.19 M ops/sec** | **~108.9 ns / event** | Kernel `epoll` + Lock-free RingBuffer via Panama FFM |
| **Netty (NIO EventLoop)** | ~7.56 M ops/sec | ~132.3 ns / event | `Selector` + ConcurrentLinkedQueue dispatch |
| **Project Reactor (Schedulers)** | ~7.51 M ops/sec | ~133.2 ns / event | RingBuffer-backed Schedulers |
| **SynchronousQueue** | ~6.81 M ops/sec | ~146.9 ns / event | Dual stack / queue thread handoff |
| **Vert.x (Event Loop)** | ~5.64 M ops/sec | ~177.2 ns / event | Netty-backed event loop dispatch |
| **Java Virtual Threads (Loom)** | ~4.23 M ops/sec | ~236.2 ns / event | Carrier thread park/unpark overhead |
<!-- BENCHMARK_TABLE_END -->

## Feature Comparison

| Feature | zThread | Netty | Virtual Threads |
| :--- | :--- | :--- | :--- |
| epoll | ✅ | ✅ | JVM |
| eventfd | ✅ | ❌ | ❌ |
| timerfd | ✅ | ❌ | ❌ |
| signalfd | ✅ | ❌ | ❌ |
| inotify | ✅ | ❌ | ❌ |
| Single Event Loop | ✅ | ✅ | ❌ |

## Installation

zThread is available on Maven Central. 

Add the `zthread-linux` dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.namanoncode</groupId>
    <artifactId>zthread-linux</artifactId>
    <version>1.0.0</version>
</dependency>
```

> [!IMPORTANT]  
> Because zThread utilizes the Foreign Function & Memory API (FFM) for native Linux syscalls, you must run your JVM with `--enable-native-access=ALL-UNNAMED` and require Java 21+ (Java 25 is currently targeted in the project). We also recommend using ZGC (`-XX:+UseZGC`).

## Reactor Integration (Optional)

If you are building reactive applications, you can bridge zThread with Project Reactor streams:

```xml
<dependency>
    <groupId>io.github.namanoncode</groupId>
    <artifactId>zthread-reactor</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
ZEventFlux.fromRuntime(runtime, CustomEvent.class)
    .filter(event -> event.payload() != null)
    .map(event -> event.payload().toString())
    .subscribe(payload -> System.out.println("Reactive Payload: " + payload));
```

## Documentation & Contributing

Full documentation, including API references, architecture deep-dives, and contribution guidelines, is available in the [zThread GitHub Wiki](https://github.com/namanONcode/zThread/wiki).

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to set up the repository locally.
