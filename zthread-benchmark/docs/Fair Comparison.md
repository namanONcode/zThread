# Fair Comparison Guidelines

This benchmark suite goes to extreme lengths to ensure that no framework receives an unfair advantage. 

## Thread Allocation
Each framework adapter is given exactly the same number of consumer threads to process events. For example, if a scenario is 16 Producers to 4 Consumers (16:4):
- The `BlockingQueueAdapter` spins up exactly 4 worker threads.
- The `NettyAdapter` uses a 4-thread `EventLoopGroup`.
- The `ReactorAdapter` uses `Schedulers.newParallel("...", 4)`.
- The `ZThreadAdapter` is instantiated via `withMaxThreads(4)`.

## Workload Isolation
- All event payloads are identical byte arrays.
- Runtimes are torn down and re-instantiated between trial iterations to prevent JVM generational heap state from leaking between implementations.
- No `offer` vs `write` API comparisons; the only metric collected is the time it takes an event to travel from the producer thread to full completion within the consumer thread's handler.
