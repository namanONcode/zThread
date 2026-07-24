package io.github.namanoncode.zthread.benchmark.timer;

import io.github.namanoncode.zthread.ZRuntime;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class TimerExecutionBenchmark {

    // Reduce batch size for timers to avoid OOM or FD exhaustion on unoptimized runtimes
    public static final int BATCH_SIZE = 50_000;
    public static final int DELAY_MS = 10;

    private ZRuntime zRuntime;
    
    private ScheduledExecutorService scheduledExecutor;
    
    private HashedWheelTimer nettyTimer;
    
    private Scheduler reactorScheduler;

    private io.vertx.core.Vertx vertx;

    @Setup(Level.Trial)
    public void setup() {
        zRuntime = ZRuntime.builder().build();
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        nettyTimer = new HashedWheelTimer();
        nettyTimer.start();
        reactorScheduler = Schedulers.single();
        vertx = io.vertx.core.Vertx.vertx();
    }

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException {
        zRuntime.shutdown();
        zRuntime.awaitTermination(5, TimeUnit.SECONDS);

        scheduledExecutor.shutdown();
        scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);

        nettyTimer.stop();

        reactorScheduler.dispose();

        CountDownLatch vertxLatch = new CountDownLatch(1);
        vertx.close().onComplete(v -> vertxLatch.countDown());
        vertxLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchScheduledExecutorService(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        Runnable task = () -> {
            bh.consume(1);
            latch.countDown();
        };

        for (int i = 0; i < BATCH_SIZE; i++) {
            scheduledExecutor.schedule(task, DELAY_MS, TimeUnit.MILLISECONDS);
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchNettyHashedWheelTimer(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        TimerTask task = timeout -> {
            bh.consume(1);
            latch.countDown();
        };

        for (int i = 0; i < BATCH_SIZE; i++) {
            nettyTimer.newTimeout(task, DELAY_MS, TimeUnit.MILLISECONDS);
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchReactorScheduler(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        Runnable task = () -> {
            bh.consume(1);
            latch.countDown();
        };

        for (int i = 0; i < BATCH_SIZE; i++) {
            reactorScheduler.schedule(task, DELAY_MS, TimeUnit.MILLISECONDS);
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchVertxTimer(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        
        for (int i = 0; i < BATCH_SIZE; i++) {
            vertx.setTimer(DELAY_MS, id -> {
                bh.consume(1);
                latch.countDown();
            });
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchZThreadTimer(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        Runnable task = () -> {
            bh.consume(1);
            latch.countDown();
        };

        for (int i = 0; i < BATCH_SIZE; i++) {
            zRuntime.schedule(task, DELAY_MS, TimeUnit.MILLISECONDS);
        }
        latch.await();
    }
}
