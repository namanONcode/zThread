# Configuration

Configuration for a zThread instance is handled via the `ZRuntimeBuilder` and stored immutably in `ZRuntimeConfig`. There are no external environment variables or properties files required by the library itself.

## ZRuntimeConfig Options

| Option | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `threadName` | No | `zthread-event-loop` | The name given to the background event loop thread. |
| `bufferSize` | No | `4096` | The capacity of the MPSC ring buffer. Must be positive; the builder automatically rounds this to a minimum of `64`. |
| `maxEventsPerPoll` | No | `64` | The maximum number of events retrieved from the kernel per `epoll_wait` call. |
| `metricsEnabled` | No | `true` | Whether internal performance metrics collection is enabled. |
| `debugEnabled` | No | `false` | Whether debug-level internal logging is printed. |

### Usage Example

```java
ZRuntime runtime = ZRuntime.builder()
    .threadName("my-app-worker")
    .bufferSize(8192)
    .maxEventsPerPoll(128)
    .metricsEnabled(false)
    .build();
```

### Security Notes

The configuration options do not manage secrets or sensitive data. However, setting `bufferSize` excessively high in an environment with many concurrent event loops could lead to `OutOfMemoryError` conditions, as the buffer is allocated upfront.
