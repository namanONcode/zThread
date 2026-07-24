package io.github.namanoncode.zthread.benchmark.scenarios;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.ZRuntimeBuilder;
import io.github.namanoncode.zthread.event.TimerEvent;
import io.netty.util.HashedWheelTimer;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class S10_SchedulerBenchmark {

    public enum TimerFramework {
        ZTHREAD,
        SCHEDULED_EXECUTOR,
        NETTY_HASHED_WHEEL
    }

    @Param({"ZTHREAD", "SCHEDULED_EXECUTOR", "NETTY_HASHED_WHEEL"})
    private TimerFramework framework;

    private ZRuntime zRuntime;
    private ScheduledExecutorService executor;
    private HashedWheelTimer nettyTimer;

    @Setup(Level.Trial)
    public void setup() {
        switch (framework) {
            case ZTHREAD:
                zRuntime = ZRuntime.builder().threadName("zthread-timer").build();
                zRuntime.on(TimerEvent.class, e -> {});
                zRuntime.start();
                break;
            case SCHEDULED_EXECUTOR:
                executor = Executors.newSingleThreadScheduledExecutor();
                break;
            case NETTY_HASHED_WHEEL:
                nettyTimer = new HashedWheelTimer();
                break;
        }
    }

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException {
        if (zRuntime != null) zRuntime.close();
        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        if (nettyTimer != null) nettyTimer.stop();
    }

    @Benchmark
    public void scheduleAndCancel() {
        switch (framework) {
            case ZTHREAD:
                // Schedule and immediately cancel to test overhead
                zRuntime.schedule(() -> {}, 10, TimeUnit.SECONDS).cancel();
                break;
            case SCHEDULED_EXECUTOR:
                executor.schedule((Runnable) () -> {}, 10, TimeUnit.SECONDS).cancel(false);
                break;
            case NETTY_HASHED_WHEEL:
                nettyTimer.newTimeout(timeout -> {}, 10, TimeUnit.SECONDS).cancel();
                break;
        }
    }
}
