# zThread Wiki

Welcome to the zThread documentation. zThread is a Linux-native event runtime for Java. It provides low-latency event dispatching by interfacing directly with Linux kernel mechanisms (epoll, eventfd, timerfd, inotify) via the Foreign Function & Memory API (FFM). 

This approach bypasses traditional JVM busy-spin overhead, allowing the runtime to sleep in the kernel and wake only when work exists.

## Project Overview

Traditional Java event loops either rely on `Selector` polling (which has overhead and latency scaling issues) or busy-spinning (which consumes 100% CPU on an idle core). zThread bridges this gap for Linux environments. It creates a direct FFM interface to `epoll`, allowing a Java thread to park inside the kernel and wake up in nanoseconds when file descriptors or lock-free ring buffers signal readiness.

### Key Features

* **Native Linux Integration:** Direct use of `epoll`, `eventfd`, `timerfd`, `signalfd`, and `inotify` through Java 21+ FFM.
* **Lock-Free MPSC Ring Buffer:** Fast multi-producer, single-consumer event delivery.
* **Near-Zero Idle CPU:** The event loop parks in the kernel when idle, consuming negligible CPU cycles.
* **Low Latency:** Benchmarks show ~75-100 ns latency per event in SPSC scenarios.
* **Reactor Integration:** Optional bridge to Project Reactor for reactive programming workflows.

## Architecture Summary

The project is split into a core API module and a Linux-specific implementation module. 

1. **`zthread-core`**: Defines the public API (`ZRuntime`, `ZRuntimeBuilder`), event interfaces, and configuration classes.
2. **`zthread-linux`**: Implements the core API using native Linux syscalls via Panama FFM (`LinuxRuntime`, `LinuxEventLoop`, `LinuxNativePoller`).
3. **`zthread-reactor`**: Provides a bridge from zThread's internal event loop to Reactor `Flux` streams (`ZEventFlux`).

## Navigation

* [Getting Started](Getting-Started)
* [Architecture](Architecture)
* [Project Structure](Project-Structure)
* [API Reference](API)
* [Configuration](Configuration)
* [Development Workflow](Development)
* [Testing](Testing)
* [Security](Security)
* [Contributing](Contributing)
* [FAQ](FAQ)
* [Glossary](Glossary)
