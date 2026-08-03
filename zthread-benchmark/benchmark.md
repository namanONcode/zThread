# zThread Benchmark Results

## Scaling Throughput (64B)
![Scaling](charts/scaling_throughput.png)

## Raw Data
| Framework         | Concurrency   |   PayloadSize |   Throughput (ops/s) |   Error |
|:------------------|:--------------|--------------:|---------------------:|--------:|
| SYNCHRONOUS_QUEUE | 8:1           |            64 |          2.88712e+06 |     nan |
| THREAD_POOL       | 8:1           |            64 |          2.85072e+06 |     nan |
| FORK_JOIN         | 8:1           |            64 |          3.59033e+06 |     nan |
| VIRTUAL_THREADS   | 8:1           |            64 |          1.6e+06     |     nan |
| REACTOR           | 8:1           |            64 |          3.69897e+06 |     nan |
| NETTY             | 8:1           |            64 |          3.30496e+06 |     nan |
| ZTHREAD           | 1:1           |            64 |          3.07317e+06 |     nan |
| SYNCHRONOUS_QUEUE | 1:1           |            64 |          1.15276e+06 |     nan |
| THREAD_POOL       | 1:1           |            64 |          2.48956e+06 |     nan |
| FORK_JOIN         | 1:1           |            64 |          2.91018e+06 |     nan |
| VIRTUAL_THREADS   | 1:1           |            64 |          1.31417e+06 |     nan |
| REACTOR           | 1:1           |            64 |          3.14313e+06 |     nan |
| NETTY             | 1:1           |            64 |          3.65113e+06 |     nan |
