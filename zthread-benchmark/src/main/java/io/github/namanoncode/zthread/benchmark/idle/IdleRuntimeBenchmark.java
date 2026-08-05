package io.github.namanoncode.zthread.benchmark.idle;

import io.github.namanoncode.zthread.ZRuntime;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime) // Run once per iteration
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3) // 3 iterations
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class IdleRuntimeBenchmark {

    private ZRuntime zRuntime;
    private EventLoopGroup nettyEventLoopGroup;
    private io.vertx.core.Vertx vertx;
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService virtualThreadExecutor;

    @Setup(Level.Trial)
    public void setup() {
        zRuntime = ZRuntime.builder().build();
        nettyEventLoopGroup = new DefaultEventLoopGroup(1);
        vertx = io.vertx.core.Vertx.vertx();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException {
        zRuntime.shutdown();
        zRuntime.awaitTermination(5, TimeUnit.SECONDS);

        nettyEventLoopGroup.shutdownGracefully().await(5, TimeUnit.SECONDS);

        CountDownLatch vertxLatch = new CountDownLatch(1);
        vertx.close().onComplete(v -> vertxLatch.countDown());
        vertxLatch.await();

        scheduledExecutor.shutdown();
        scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);

        virtualThreadExecutor.shutdown();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * In an idle benchmark, the JMH worker thread must sleep so it doesn't spin and consume CPU.
     * We sleep for 10 seconds. Profilers attached to this benchmark will measure the
     * background CPU consumption of the event loop threads.
     */
    @Benchmark
    public void idleBaseline() throws InterruptedException {
        Thread.sleep(10000); // 10 seconds of pure idle
    }
}
