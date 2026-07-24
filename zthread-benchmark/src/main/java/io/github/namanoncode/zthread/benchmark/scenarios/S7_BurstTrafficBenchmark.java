package io.github.namanoncode.zthread.benchmark.scenarios;

import io.github.namanoncode.zthread.benchmark.framework.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.framework.EventHandler;
import io.github.namanoncode.zthread.benchmark.framework.adapter.*;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@Fork(1)
@State(Scope.Benchmark)
public class S7_BurstTrafficBenchmark {

    @Param({"ZTHREAD", "ARRAY_BLOCKING_QUEUE", "THREAD_POOL", "REACTOR", "NETTY", "VERTX"})
    private S1_S5_ThroughputLatencyBenchmark.Framework framework;

    @Param({"1000000"})
    private int burstSize;

    private EventRuntimeAdapter adapter;
    private CountDownLatch latch;

    @Setup(Level.Invocation)
    public void setup() {
        switch (framework) {
            case ZTHREAD:
                adapter = new ZThreadAdapter();
                break;
            case ARRAY_BLOCKING_QUEUE:
                adapter = new BlockingQueueAdapter(new java.util.concurrent.ArrayBlockingQueue<>(1_000_000));
                break;
            case THREAD_POOL:
                adapter = new ExecutorAdapter(Executors.newFixedThreadPool(4));
                break;
            case REACTOR:
                adapter = new ReactorAdapter();
                break;
            case NETTY:
                adapter = new NettyAdapter();
                break;
            case VERTX:
                adapter = new VertxAdapter();
                break;
            default:
                throw new IllegalStateException("Unsupported framework");
        }

        latch = new CountDownLatch(burstSize);
        EventHandler handler = event -> {
            if (event.validate()) {
                latch.countDown();
            }
        };

        adapter.start(handler, 4);
    }

    @TearDown(Level.Invocation)
    public void teardown() {
        adapter.shutdown();
    }

    @Benchmark
    public void burst() throws InterruptedException {
        for (int i = 0; i < burstSize; i++) {
            adapter.submit(new BenchmarkEvent(64));
            // Backpressure prevention for queues that might fill up
            if (i % 10_000 == 0) {
                LockSupport.parkNanos(1); 
            }
        }
        latch.await();
    }
}
