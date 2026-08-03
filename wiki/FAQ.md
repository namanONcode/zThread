# Frequently Asked Questions (FAQ)

### Why is zThread Linux only?
zThread relies on Linux-specific kernel features—namely `epoll`, `eventfd`, `timerfd`, and `signalfd`—to achieve its low-latency, zero-idle-CPU profile. Porting these directly to macOS (kqueue) or Windows (IOCP) would require entirely separate implementations. By focusing strictly on Linux, the framework provides the highest possible performance for standard server environments.

### Do I need Project Reactor?
No. The `zthread-core` API uses standard Java `Consumer` interfaces for event dispatching. You can use the library entirely on its own. The `zthread-reactor` module is strictly optional for developers who want to map zThread events into reactive `Flux` streams.

### How does zThread compare to Netty or Vert.x?
Netty and Vert.x are full-featured networking frameworks that include event loops as part of their architecture. zThread is a standalone event loop primitive. If you are building a custom protocol or high-throughput message bus where you want absolute control over the event loop and zero JVM polling overhead, zThread is an alternative to standard Java concurrent queues or spinning threads.

### Why does it require Java 21+ (or Java 25)?
zThread uses the Foreign Function & Memory (FFM) API, which finalized in Java 22 (previewed in 21). We target Java 25 to ensure the most stable and performant native bindings. This allows zThread to avoid the performance overhead and boilerplate of traditional Java Native Interface (JNI).

### What happens if the MPSC Ring Buffer fills up?
By default, calling `ZRuntime.post(event)` will throw an exception if the ring buffer is full. If you need to handle backpressure safely, use `ZRuntime.tryPost(event)`, which returns a boolean. If it returns `false`, your application can choose to drop the event, retry later, or slow down the producer.
