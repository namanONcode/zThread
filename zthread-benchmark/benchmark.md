# zThread Benchmark Results

## Scaling Throughput (64B)
![Scaling](charts/scaling_throughput.png)

## Raw Data
| Framework         | Concurrency   |   PayloadSize |   Throughput (ops/s) |   Error |
|:------------------|:--------------|--------------:|---------------------:|--------:|
| ZTHREAD           | 8:1           |            64 |          2.91285e+06 |     nan |
| SYNCHRONOUS_QUEUE | 8:1           |            64 |          1.68178e+06 |     nan |
| THREAD_POOL       | 8:1           |            64 |          3.26946e+06 |     nan |
| FORK_JOIN         | 8:1           |            64 |          4.03829e+06 |     nan |
| VIRTUAL_THREADS   | 8:1           |            64 |          1.57165e+06 |     nan |
| REACTOR           | 8:1           |            64 |          3.7419e+06  |     nan |
| NETTY             | 8:1           |            64 |          3.69305e+06 |     nan |
| SYNCHRONOUS_QUEUE | 1:1           |            64 |          3.4084e+06  |     nan |
| THREAD_POOL       | 1:1           |            64 |          3.0927e+06  |     nan |
| FORK_JOIN         | 1:1           |            64 |          3.42426e+06 |     nan |
| VIRTUAL_THREADS   | 1:1           |            64 |          1.53249e+06 |     nan |
| REACTOR           | 1:1           |            64 |          3.83861e+06 |     nan |
| NETTY             | 1:1           |            64 |          3.40941e+06 |     nan |
