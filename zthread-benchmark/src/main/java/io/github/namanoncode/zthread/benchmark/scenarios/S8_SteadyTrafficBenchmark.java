package io.github.namanoncode.zthread.benchmark.scenarios;

import io.github.namanoncode.zthread.benchmark.framework.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.framework.EventHandler;
import io.github.namanoncode.zthread.benchmark.framework.adapter.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

@BenchmarkMode(Mode.SampleTime) // Sample time helps us get percentiles (P99 latency)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class S8_SteadyTrafficBenchmark {

    @Param({"ZTHREAD", "ARRAY_BLOCKING_QUEUE", "THREAD_POOL", "REACTOR", "NETTY", "VERTX"})
    private S1_S5_ThroughputLatencyBenchmark.Framework framework;

    @Param({"1000", "5000", "10000"}) // Target events per second
    private int targetRate;

    private EventRuntimeAdapter adapter;
    private long sleepNanos;

    @Setup(Level.Trial)
    public void setup() {
        sleepNanos = 1_000_000_000L / targetRate;
        switch (framework) {
            case ZTHREAD:
                adapter = new ZThreadAdapter();
                break;
            case ARRAY_BLOCKING_QUEUE:
                adapter = new BlockingQueueAdapter(new java.util.concurrent.ArrayBlockingQueue<>(10_000));
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

        EventHandler handler = event -> {
            event.validate();
        };

        adapter.start(handler, 4);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        adapter.shutdown();
    }

    @Benchmark
    public void steadySubmit(Blackhole bh) {
        long start = System.nanoTime();
        adapter.submit(new BenchmarkEvent(64));
        long elapsed = System.nanoTime() - start;
        
        // Sleep to maintain steady traffic rate
        long toSleep = sleepNanos - elapsed;
        if (toSleep > 0) {
            LockSupport.parkNanos(toSleep);
        }
    }
}
