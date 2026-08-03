# Troubleshooting

This page documents common issues encountered when running or developing zThread, along with their resolutions.

## 1. `IllegalCallerException` on Startup

**Symptom:**
```text
Exception in thread "main" java.lang.IllegalCallerException: 
WARNING: A restricted method in java.lang.foreign.Linker has been called
```

**Cause:**
You forgot to pass the native access flag to the JVM. zThread requires explicit permission to call native C functions like `epoll_create1`.

**Resolution:**
Add `--enable-native-access=ALL-UNNAMED` to your `java` execution command or your IDE's VM arguments.

## 2. `UnsatisfiedLinkError` or Unsupported OS

**Symptom:**
```text
java.lang.UnsatisfiedLinkError: unresolved symbol: epoll_create1
```
*or* 
```text
ConfigurationException: No ZRuntime implementation found
```

**Cause:**
You are trying to run zThread on an operating system other than Linux (e.g., macOS or Windows). zThread relies on Linux-specific kernel mechanisms.

**Resolution:**
Run your application on a Linux environment, a Linux Docker container, or WSL2.

## 3. High CPU Usage

**Symptom:**
Your application shows 100% CPU usage on a core when idle.

**Cause:**
This indicates the event loop is spinning rather than parking. This usually means a bug in event dispatching where `epoll_wait` is returning immediately, or an `eventfd` was signaled but not properly read/drained, causing `epoll` to continually trigger a level-triggered event.

**Resolution:**
Enable debug logging via `ZRuntimeBuilder.debugEnabled(true)` to trace the `epoll_wait` return counts. If the issue persists in unmodified library code, please open a GitHub issue.
