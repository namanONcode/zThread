# zThread
> A Linux-native Event Runtime for Java providing near-zero idle CPU utilization using kernel event mechanisms (epoll, eventfd, timerfd, signalfd, inotify).

zThread is an enterprise-grade, production-quality, high-performance event loop framework tailored exclusively for Linux. By leveraging `epoll`, `eventfd`, and FFM (Foreign Function & Memory API) directly, zThread eliminates the JVM busy-spin overhead and provides an ultra-low latency event dispatch model that sleeps inside the kernel and wakes only when work exists.

## Performance Benchmark

zThread is designed for maximum throughput and minimal latency. In single-producer single-consumer (SPSC) event loop benchmarks executed with JMH on Linux (JDK 25), zThread achieves **~13.2 Million ops/sec** (~75.7 ns latency per event), outperforming traditional NIO frameworks like Netty (~7.2 M ops/sec) by **1.8x** and Vert.x (~8.3 M ops/sec) by **1.6x**.

![Benchmark Results](assets/benchmark_graph.svg)

*(Graph automatically benchmarked and generated via GitHub Actions CI)*

### Benchmark Breakdown & Latency Matrix

<!-- BENCHMARK_TABLE_START -->
| Framework / Mechanism | Throughput (Higher is better) | Average Latency (Lower is better) | Engine Architecture |
| :--- | :--- | :--- | :--- |
| **ConcurrentLinkedQueue** | ~20.22 M ops/sec | ~49.5 ns / event | Lock-free queue algorithm |
| **ArrayBlockingQueue** | ~19.83 M ops/sec | ~50.4 ns / event | ReentrantLock + Condition queues |
| **LinkedBlockingQueue** | ~12.33 M ops/sec | ~81.1 ns / event | Two-lock queue algorithm |
| **zThread (Linux FFM / Epoll)** | **~11.72 M ops/sec** | **~85.3 ns / event** | Kernel `epoll` + Lock-free RingBuffer via Panama FFM |
| **SynchronousQueue** | ~11.04 M ops/sec | ~90.6 ns / event | Dual stack / queue thread handoff |
| **Netty (NIO EventLoop)** | ~11.01 M ops/sec | ~90.8 ns / event | `Selector` + ConcurrentLinkedQueue dispatch |
| **Project Reactor (Schedulers)** | ~9.15 M ops/sec | ~109.3 ns / event | RingBuffer-backed Schedulers |
| **Vert.x (Event Loop)** | ~8.34 M ops/sec | ~119.9 ns / event | Netty-backed event loop dispatch |
| **Java Virtual Threads (Loom)** | ~4.91 M ops/sec | ~203.7 ns / event | Carrier thread park/unpark overhead |
| **benchmark** | ~4.34 M ops/sec | ~230.4 ns / event | Unknown architecture |
| **benchmark** | ~4.04 M ops/sec | ~247.6 ns / event | Unknown architecture |
| **benchmark** | ~3.96 M ops/sec | ~252.7 ns / event | Unknown architecture |
| **benchmark** | ~3.76 M ops/sec | ~266.3 ns / event | Unknown architecture |
| **benchmark** | ~3.69 M ops/sec | ~270.8 ns / event | Unknown architecture |
| **benchmark** | ~3.68 M ops/sec | ~271.4 ns / event | Unknown architecture |
| **benchmark** | ~3.62 M ops/sec | ~276.0 ns / event | Unknown architecture |
| **benchmark** | ~3.61 M ops/sec | ~276.8 ns / event | Unknown architecture |
| **benchmark** | ~3.27 M ops/sec | ~305.9 ns / event | Unknown architecture |
| **benchmark** | ~3.02 M ops/sec | ~330.9 ns / event | Unknown architecture |
| **benchmark** | ~2.98 M ops/sec | ~335.6 ns / event | Unknown architecture |
| **benchmark** | ~2.89 M ops/sec | ~345.7 ns / event | Unknown architecture |
| **benchmark** | ~2.88 M ops/sec | ~347.8 ns / event | Unknown architecture |
| **benchmark** | ~2.81 M ops/sec | ~355.4 ns / event | Unknown architecture |
| **benchmark** | ~2.71 M ops/sec | ~369.6 ns / event | Unknown architecture |
| **benchmark** | ~1.34 M ops/sec | ~746.6 ns / event | Unknown architecture |
| **benchmark** | ~1.06 M ops/sec | ~940.4 ns / event | Unknown architecture |
| **benchmark** | ~0.75 M ops/sec | ~1340.4 ns / event | Unknown architecture |
| **benchmark** | ~0.56 M ops/sec | ~1784.6 ns / event | Unknown architecture |
<!-- BENCHMARK_TABLE_END -->

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
> Because zThread heavily utilizes the **Foreign Function & Memory API (FFM)** for near-zero allocation and native Linux syscalls, you **must** run your JVM with `--enable-native-access=ALL-UNNAMED` and require Java 21+ (Java 25 is currently targeted in the project). We also recommend using ZGC (`-XX:+UseZGC`).

## Getting Started

zThread is incredibly simple to embed.

### 1. Create and Configure the Runtime
Use the builder pattern to configure your core event loop limits and behaviors:

```java
import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.event.CustomEvent;

public class App {
    public static void main(String[] args) throws InterruptedException {
        // 1. Initialize Runtime
        ZRuntime runtime = ZRuntime.builder()
            .threadName("zthread-worker-1")
            .bufferSize(8192)          // Size of the lock-free MPSC ring buffer
            .metricsEnabled(true)
            .build();

        // 2. Register Event Handlers
        runtime.on(CustomEvent.class, event -> {
            System.out.println("Received event: " + event.payload());
        });

        // 3. Start the Event Loop
        runtime.start();

        // 4. Post Events (from any thread)
        // This executes a lock-free ring buffer insertion. 
        // We also provide tryPost() for non-throwing backpressure handling.
        runtime.post(new CustomEvent("Hello from main thread!"));

        // Graceful shutdown
        Thread.sleep(100);
        runtime.shutdown();
    }
}
```

### Reactor Integration (Optional)

If you're building reactive applications, you can bridge zThread with Project Reactor streams seamlessly:

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

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md) for how to set up the repository locally.

