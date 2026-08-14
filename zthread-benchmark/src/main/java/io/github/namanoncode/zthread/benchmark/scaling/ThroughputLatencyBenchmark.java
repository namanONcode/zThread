package io.github.namanoncode.zthread.benchmark.scaling;

import io.github.namanoncode.zthread.benchmark.adapters.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.adapters.EventHandler;
import io.github.namanoncode.zthread.benchmark.adapters.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ThroughputLatencyBenchmark {

    public enum Framework {
        ZTHREAD,
        SYNCHRONOUS_QUEUE,
        THREAD_POOL,
        FORK_JOIN,
        VIRTUAL_THREADS,
        REACTOR,
        NETTY,
        VERTX
    }

    @Param({"ZTHREAD", "VIRTUAL_THREADS", "NETTY", "VERTX"})
    private Framework framework;

    @Param({"64", "1024"})
    private int payloadSize;

    // Concurrency combinations: "Producers:Consumers"
    @Param({"1:1", "8:1", "32:8"})
    private String concurrency;

    private int producers;
    private int consumers;

    private EventRuntimeAdapter adapter;
    private AtomicLong processedCount;

    @Setup(Level.Trial)
    public void setup() {
        String[] parts = concurrency.split(":");
        producers = Integer.parseInt(parts[0]);
        consumers = Integer.parseInt(parts[1]);
        processedCount = new AtomicLong(0);

        switch (framework) {
            case ZTHREAD:
                adapter = new ZThreadAdapter();
                break;
            case SYNCHRONOUS_QUEUE:
                adapter = new BlockingQueueAdapter(new SynchronousQueue<>());
                break;
            case THREAD_POOL:
                adapter = new ExecutorAdapter(Executors.newFixedThreadPool(consumers));
                break;
            case FORK_JOIN:
                adapter = new ExecutorAdapter(new ForkJoinPool(consumers));
                break;
            case VIRTUAL_THREADS:
                adapter = new VirtualThreadAdapter();
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
        }

        EventHandler handler = event -> {
            if (event.validate()) {
                processedCount.incrementAndGet();
            }
        };

        adapter.start(handler, consumers);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        adapter.shutdown();
    }

    @Benchmark
    public void benchmark(Blackhole bh) {
        BenchmarkEvent event = new BenchmarkEvent(payloadSize);
        adapter.submit(event);
        bh.consume(event);
    }
}
