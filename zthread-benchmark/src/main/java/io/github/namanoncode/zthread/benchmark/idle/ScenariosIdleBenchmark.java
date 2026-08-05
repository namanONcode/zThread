package io.github.namanoncode.zthread.benchmark.idle;

import io.github.namanoncode.zthread.benchmark.adapters.EventHandler;
import io.github.namanoncode.zthread.benchmark.adapters.*;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.*;

@BenchmarkMode(Mode.SingleShotTime) // Measures the duration of the benchmark method
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 3, batchSize = 1) // 3 iterations of 1 second each
@Fork(1)
@State(Scope.Benchmark)
public class ScenariosIdleBenchmark {

    public enum Framework { ZTHREAD, THREAD_POOL, REACTOR, NETTY, VERTX }

    @Param({"ZTHREAD", "THREAD_POOL", "REACTOR", "NETTY", "VERTX"})
    private Framework framework;

    private EventRuntimeAdapter adapter;

    @Setup(Level.Iteration)
    public void setup() {
        switch (framework) {
            case ZTHREAD:
                adapter = new ZThreadAdapter();
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
                throw new IllegalStateException("Unsupported framework for idle benchmark");
        }

        // Dummy handler, won't be called
        EventHandler handler = event -> {};
        adapter.start(handler, 4);
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        adapter.shutdown();
    }

    @Benchmark
    public void idleForOneSecond() throws InterruptedException {
        // Sleep for 1 second. This allows the async-profiler (if attached)
        // to record context switches, voluntary/involuntary switches, and CPU usage
        // of the idle threads created by the framework.
        Thread.sleep(1000);
    }
}
