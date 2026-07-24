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
| **zThread (Linux FFM / Epoll)** | **~13.21 M ops/sec** | **~75.7 ns / event** | Kernel `epoll` + Lock-free RingBuffer via Panama FFM |
| **Project Reactor** | ~10.68 M ops/sec | ~93.6 ns / event | RingBuffer-backed Schedulers |
| **SynchronousQueue** | ~10.34 M ops/sec | ~96.7 ns / event | Dual stack / queue thread handoff |
| **ArrayBlockingQueue** | ~10.00 M ops/sec | ~100.0 ns / event | ReentrantLock + Condition queues |
| **LinkedBlockingQueue** | ~9.88 M ops/sec | ~101.2 ns / event | Two-lock queue algorithm |
| **Vert.x (Event Loop)** | ~8.32 M ops/sec | ~120.2 ns / event | Netty-backed event loop dispatch |
| **Netty (NIO EventLoop)** | ~7.21 M ops/sec | ~138.7 ns / event | `Selector` + ConcurrentLinkedQueue dispatch |
| **Java Virtual Threads** | ~1.12 M ops/sec | ~895.2 ns / event | Carrier thread park/unpark overhead |
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

