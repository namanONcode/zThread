# Changelog

All notable changes to zThread will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Core event runtime API with `ZRuntime`, `EventLoop`, `EventDispatcher`, and `Scheduler`
- Linux native bindings via FFM API for epoll, eventfd, timerfd, signalfd, and inotify
- Lock-free MPSC ring buffer for zero-allocation event posting
- Object pooling for reusable event instances
- Type-safe event dispatch without reflection
- Socket event support (TCP server/client)
- File system watching via inotify
- Signal handling via signalfd
- Timer scheduling (immediate, delayed, periodic, one-shot)
- Custom event posting with eventfd-based wakeup
- Runtime metrics collection (loop latency, events processed, wakeups, handler time)
- Project Reactor integration module (Flux/Mono bridges, Scheduler)
- JMH benchmarks comparing against BlockingQueue, ExecutorService, and Reactor
- Working examples for all features
- Comprehensive test suite with unit, integration, and stress tests
- GitHub Actions CI/CD pipeline
- Full documentation (architecture, developer guide, performance, threading)

[Unreleased]: https://github.com/namanoncode/zThread/commits/main
