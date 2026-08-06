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
public class MpscEventBenchmark {

    public static final int BATCH_SIZE = 1_000_000;
    public static final int PRODUCERS = 4;
    public static final int EVENTS_PER_PRODUCER = BATCH_SIZE / PRODUCERS;

    // Standard Queues
    private ArrayBlockingQueue<Object> arrayQueue;
    private LinkedBlockingQueue<Object> linkedQueue;
    private ConcurrentLinkedQueue<Object> concurrentQueue;
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
    private ConcurrentLinkedQueue<Object> activeConcurrentQueue;

    // Producer thread pool
    private ExecutorService producerPool;

    @Setup(Level.Trial)
    public void setup() {
        arrayQueue = new ArrayBlockingQueue<>(BATCH_SIZE);
        linkedQueue = new LinkedBlockingQueue<>(BATCH_SIZE);
        concurrentQueue = new ConcurrentLinkedQueue<>();
        synchronousQueue = new SynchronousQueue<>();

        zRuntime = ZRuntime.builder().build();
        zRuntime.start();
        nettyEventLoop = new DefaultEventLoop();
        vertx = io.vertx.core.Vertx.vertx();
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        producerPool = Executors.newFixedThreadPool(PRODUCERS);
    }

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
        
        producerPool.shutdown();
        producerPool.awaitTermination(5, TimeUnit.SECONDS);
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
        activeConcurrentQueue = null;
    }

    private void runProducers(Runnable producerTask) throws InterruptedException {
        CountDownLatch producersDone = new CountDownLatch(PRODUCERS);
        for (int p = 0; p < PRODUCERS; p++) {
            producerPool.submit(() -> {
                try {
                    producerTask.run();
                } finally {
                    producersDone.countDown();
                }
            });
        }
        producersDone.await();
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

    private void startConcurrentQueueConsumer(ConcurrentLinkedQueue<Object> queue, Blackhole bh) {
        activeConcurrentQueue = queue;
        queueConsumerThread = new Thread(() -> {
            while (running) {
                Object obj = activeConcurrentQueue.poll();
                if (obj != null) {
                    bh.consume(obj);
                    queueLatch.countDown();
                }
            }
        });
        queueConsumerThread.start();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchArrayBlockingQueue(Blackhole bh) throws InterruptedException {
        startBlockingQueueConsumer(arrayQueue, bh);
        runProducers(() -> {
            try {
                for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                    arrayQueue.put(EVENT);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        queueLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchLinkedBlockingQueue(Blackhole bh) throws InterruptedException {
        startBlockingQueueConsumer(linkedQueue, bh);
        runProducers(() -> {
            try {
                for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                    linkedQueue.put(EVENT);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        queueLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchSynchronousQueue(Blackhole bh) throws InterruptedException {
        startBlockingQueueConsumer(synchronousQueue, bh);
        runProducers(() -> {
            try {
                for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                    synchronousQueue.put(EVENT);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        queueLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchConcurrentLinkedQueue(Blackhole bh) throws InterruptedException {
        startConcurrentQueueConsumer(concurrentQueue, bh);
        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                concurrentQueue.offer(EVENT);
            }
        });
        queueLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchZThread(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        io.github.namanoncode.zthread.handler.HandlerRegistration reg = zRuntime.on(io.github.namanoncode.zthread.event.CustomEvent.class, evt -> {
            bh.consume(evt);
            latch.countDown();
        });

        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                while (!zRuntime.tryPost(EVENT)) {
                    java.util.concurrent.locks.LockSupport.parkNanos(10);
                }
            }
        });
        latch.await();
        reg.cancel();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchNetty(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                nettyEventLoop.execute(() -> {
                    bh.consume(EVENT);
                    latch.countDown();
                });
            }
        });
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchVirtualThreads(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                virtualThreadExecutor.submit(() -> {
                    bh.consume(EVENT);
                    latch.countDown();
                });
            }
        });
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

        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                reactor.core.publisher.Sinks.EmitResult res;
                do {
                    res = reactorSink.tryEmitNext(EVENT);
                    if (res != reactor.core.publisher.Sinks.EmitResult.OK) {
                        java.util.concurrent.locks.LockSupport.parkNanos(10);
                    }
                } while (res != reactor.core.publisher.Sinks.EmitResult.OK);
            }
        });
        latch.await();
    }

}
