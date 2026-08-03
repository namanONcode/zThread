# Security

This document outlines the security considerations specific to using the zThread library. For general vulnerability reporting, see `SECURITY.md` in the repository root.

## Foreign Function & Memory (FFM) Risks

Because zThread directly interfaces with the Linux kernel using the Java FFM API, it requires the `--enable-native-access=ALL-UNNAMED` JVM flag. This flag disables Java's default memory safety boundaries for the library.

### Memory Safety
The `LinuxNativePoller` allocates off-heap memory (e.g., for `epoll_event` structs). While the library uses modern `Arena` and memory segment scoping to ensure memory is freed when the event loop shuts down, modifying the internal library code incorrectly can lead to:
* **Memory Leaks:** Forgetting to close an `Arena` or freeing unmanaged memory.
* **Segmentation Faults:** Attempting to read or write to an off-heap memory segment after it has been deallocated.

### File Descriptor Exhaustion
The library creates several native file descriptors (`epoll`, `eventfd`). If an application creates thousands of `ZRuntime` instances (which is an anti-pattern), it could exhaust the system's open file limit (`ulimit -n`), causing the application or OS to fail. A standard application should only initialize a small pool of `ZRuntime` instances (often just one per CPU core).

## Threat Model (Not Applicable)

Because zThread is a lower-level concurrency primitive, it is unaware of network payloads, user authentication, or cryptographic secrets. 

* **Network Security (TLS/SSL):** Must be handled by your socket library (e.g., Java NIO, Netty).
* **Input Validation:** Must be handled by the application logic before posting an event to the `ZRuntime`.
