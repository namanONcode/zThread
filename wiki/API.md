# API Reference

Because zThread is an event loop library, its API consists of Java interfaces rather than REST endpoints. This page outlines the core public interfaces developers use to interact with the runtime.

## ZRuntime

`ZRuntime` is the primary interface for starting the loop, posting events, and registering handlers.

### `void start()`
* **Purpose:** Starts the background event loop thread.
* **Side effects:** Spawns a new thread that will block in native code (`epoll_wait`) until events occur.

### `void shutdown()`
* **Purpose:** Signals the event loop to stop processing and exit gracefully.

### `<T> void on(Class<T> eventType, Consumer<T> handler)`
* **Purpose:** Registers a callback handler for a specific event class.
* **Parameters:**
  * `eventType`: The class type of the event.
  * `handler`: The consumer function to execute when the event is processed by the loop.

### `boolean post(Object event)`
* **Purpose:** Inserts an event into the MPSC ring buffer and wakes up the event loop if it is sleeping.
* **Parameters:** 
  * `event`: The object to pass to the registered handler.
* **Return value:** `true` if the event was successfully queued, `false` if the buffer is full (or throws an exception depending on backpressure configuration).

### `boolean tryPost(Object event)`
* **Purpose:** A non-blocking variant of `post` that returns `false` immediately if the ring buffer is at capacity, allowing the caller to handle backpressure without exceptions.

## ZRuntimeBuilder

A fluent builder accessed via `ZRuntime.builder()`.

* `threadName(String name)`: Sets the name of the background thread.
* `bufferSize(int size)`: Sets the capacity of the MPSC ring buffer (must be a power of two).
* `maxEventsPerPoll(int max)`: Configures how many events `epoll_wait` can retrieve in a single wakeup.
* `metricsEnabled(boolean enabled)`: Toggles internal JMX/micrometer metrics.
* `build()`: Uses `ServiceLoader` to locate the `zthread-linux` implementation and returns a configured `ZRuntime`.

## ZEventFlux (zthread-reactor)

Provides integration with Project Reactor.

### `static <T> Flux<T> fromRuntime(ZRuntime runtime, Class<T> eventType)`
* **Purpose:** Creates a reactive `Flux` that emits items whenever the underlying `ZRuntime` processes an event of `eventType`.
