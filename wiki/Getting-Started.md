# Getting Started

This guide covers how to add zThread to your project, configure the JVM, and run a basic event loop.

## Prerequisites

* **OS:** Linux (Requires epoll and eventfd)
* **JDK:** Java 25 (Java 21+ required for FFM, but this project targets JDK 25)
* **JVM Flags:** `--enable-native-access=ALL-UNNAMED` is strictly required.

## Installation

Add the `zthread-linux` dependency to your Maven `pom.xml`. The `zthread-core` module is brought in transitively.

```xml
<dependency>
    <groupId>io.github.namanoncode</groupId>
    <artifactId>zthread-linux</artifactId>
    <version>1.0.0</version>
</dependency>
```

If you plan to use Project Reactor integration:

```xml
<dependency>
    <groupId>io.github.namanoncode</groupId>
    <artifactId>zthread-reactor</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Running Locally

Because zThread uses the Foreign Function & Memory (FFM) API to interact directly with Linux shared libraries, you must run your application with native access enabled. 

```bash
java --enable-native-access=ALL-UNNAMED -cp target/classes:target/dependency/* com.your.App
```

Without this flag, the JVM will throw an `IllegalCallerException` when zThread attempts to allocate native memory or link to `epoll_wait`.

## Basic Usage

The primary entry point is the `ZRuntime` interface. You configure it using `ZRuntimeBuilder`.

```java
import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.event.CustomEvent;

public class ExampleApp {
    public static void main(String[] args) throws InterruptedException {
        // 1. Configure and build the runtime
        ZRuntime runtime = ZRuntime.builder()
            .threadName("zthread-worker")
            .bufferSize(8192) // MPSC ring buffer size
            .build();

        // 2. Register a handler for a specific event type
        runtime.on(String.class, message -> {
            System.out.println("Received string: " + message);
        });

        // 3. Start the background event loop thread
        runtime.start();

        // 4. Post events from any thread
        runtime.post("Hello from the main thread");
        runtime.post("This is a low-latency event");

        // 5. Shutdown the runtime cleanly
        Thread.sleep(100);
        runtime.shutdown();
    }
}
```

## Running Tests

To run the project's own test suite locally:

```bash
./mvnw clean verify --enable-native-access=ALL-UNNAMED
```

See the [Testing](Testing) documentation for more details on benchmarks and integration tests.
