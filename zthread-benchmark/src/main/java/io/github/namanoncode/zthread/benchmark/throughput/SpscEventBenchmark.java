package io.github.namanoncode.zthread.benchmark.throughput;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 2)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class SpscEventBenchmark {

    public static final int BATCH_SIZE = 1_000_000;

    // Standard Queue
    private SynchronousQueue<Object> synchronousQueue;

    // Async Runtimes
    private ZRuntime zRuntime;
    private EventLoop nettyEventLoop;
    private io.vertx.core.Vertx vertx;
    
    // Reactor
    private Sinks.Many<Object> reactorSink;

    // Virtual Threads
    private ExecutorService virtualThreadExecutor;

    private static final io.github.namanoncode.zthread.event.CustomEvent EVENT = new io.github.namanoncode.zthread.event.CustomEvent("bench");

    // Consumer thread for raw queues
    private volatile boolean running;
    private Thread queueConsumerThread;
    private CountDownLatch queueLatch;
    private BlockingQueue<Object> activeBlockingQueue;

    @Setup(Level.Trial)
    public void setup() {
        synchronousQueue = new SynchronousQueue<>();

        // Setup zThread with enough buffer for the batch to prevent EventLoopException: buffer full
        zRuntime = ZRuntime.builder().bufferSize(BATCH_SIZE).build();
        zRuntime.on(io.github.namanoncode.zthread.event.CustomEvent.class, evt -> {
            Blackhole bh = zBlackhole;
            if (bh != null) {
                bh.consume(evt);
            }
            CountDownLatch latch = zLatch;
            if (latch != null) {
                latch.countDown();
            }
        });
        zRuntime.start();

        // Setup Netty
        nettyEventLoop = new DefaultEventLoop();

        // Setup Vertx
        vertx = io.vertx.core.Vertx.vertx();

        // Setup Virtual Threads
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    private volatile CountDownLatch zLatch;
    private volatile Blackhole zBlackhole;

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException {
        zRuntime.shutdown();
        zRuntime.awaitTermination(5, TimeUnit.SECONDS);

        nettyEventLoop.shutdownGracefully().await(5, TimeUnit.SECONDS);
        
        CountDownLatch vertxLatch = new CountDownLatch(1);
        vertx.close().onComplete(v -> vertxLatch.countDown());
        vertxLatch.await();

        virtualThreadExecutor.shutdown();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        queueLatch = new CountDownLatch(BATCH_SIZE);
        running = true;
    }

    @TearDown(Level.Invocation)
    public void teardownInvocation() throws InterruptedException {
        running = false;
        if (queueConsumerThread != null) {
            queueConsumerThread.interrupt();
            queueConsumerThread.join();
            queueConsumerThread = null;
        }
        activeBlockingQueue = null;
    }

    private void startBlockingQueueConsumer(BlockingQueue<Object> queue, Blackhole bh) {
        activeBlockingQueue = queue;
        queueConsumerThread = new Thread(() -> {
            try {
                while (running) {
                    Object obj = activeBlockingQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (obj != null) {
                        bh.consume(obj);
                        queueLatch.countDown();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        queueConsumerThread.start();
    }





    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchSynchronousQueue(Blackhole bh) throws InterruptedException {
        startBlockingQueueConsumer(synchronousQueue, bh);
        for (int i = 0; i < BATCH_SIZE; i++) {
            synchronousQueue.put(EVENT);
        }
        queueLatch.await();
    }



    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchZThread(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        this.zLatch = latch;
        this.zBlackhole = bh;

        for (int i = 0; i < BATCH_SIZE; i++) {
            while (!zRuntime.tryPost(EVENT)) {
                Thread.onSpinWait();
            }
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchNetty(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            nettyEventLoop.execute(() -> {
                bh.consume(EVENT);
                latch.countDown();
            });
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchVirtualThreads(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            virtualThreadExecutor.submit(() -> {
                bh.consume(EVENT);
                latch.countDown();
            });
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchReactor(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        reactorSink = reactor.core.publisher.Sinks.many().unicast().onBackpressureBuffer();
        
        reactorSink.asFlux()
            .publishOn(Schedulers.single())
            .subscribe(evt -> {
                bh.consume(evt);
                latch.countDown();
            });

        for (int i = 0; i < BATCH_SIZE; i++) {
            reactor.core.publisher.Sinks.EmitResult res;
            do {
                res = reactorSink.tryEmitNext(EVENT);
                if (res != reactor.core.publisher.Sinks.EmitResult.OK) {
                    java.util.concurrent.locks.LockSupport.parkNanos(10);
                }
            } while (res != reactor.core.publisher.Sinks.EmitResult.OK);
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchVertx(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        io.vertx.core.eventbus.MessageConsumer<Object> consumer = vertx.eventBus().localConsumer("benchmark.address");
        try {
            consumer.getClass().getMethod("setMaxBufferedMessages", int.class).invoke(consumer, 10_000_000);
        } catch (Exception e) {
            // Ignore if missing in newer Vert.x versions
        }
        consumer.handler(msg -> {
            bh.consume(msg.body());
            latch.countDown();
        });

        // Wait for consumer to be registered to avoid dropping messages
        CountDownLatch regLatch = new CountDownLatch(1);
        try {
            java.lang.reflect.Method m = consumer.getClass().getMethod("completionHandler", io.vertx.core.Handler.class);
            m.invoke(consumer, (io.vertx.core.Handler<io.vertx.core.AsyncResult<Void>>) res -> regLatch.countDown());
            regLatch.await();
        } catch (Exception e) {
            // Fallback for newer Vert.x versions
            Thread.sleep(100);
        }

        for (int i = 0; i < BATCH_SIZE; i++) {
            vertx.eventBus().send("benchmark.address", "bench");
        }
        latch.await();
        consumer.unregister().toCompletionStage().toCompletableFuture().join();
    }
}
