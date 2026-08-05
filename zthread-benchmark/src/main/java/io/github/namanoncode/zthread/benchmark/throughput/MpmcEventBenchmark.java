package io.github.namanoncode.zthread.benchmark.throughput;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class MpmcEventBenchmark {

    public static final int BATCH_SIZE = 1_000_000;
    public static final int PRODUCERS = 16;
    public static final int CONSUMERS = 4;
    public static final int EVENTS_PER_PRODUCER = BATCH_SIZE / PRODUCERS;

    // Standard Queues
    private ArrayBlockingQueue<Object> arrayQueue;
    private LinkedBlockingQueue<Object> linkedQueue;
    private ConcurrentLinkedQueue<Object> concurrentQueue;

    // Async Runtimes
    private ZRuntime[] zRuntimes;
    private EventLoopGroup nettyEventLoopGroup;
    private io.vertx.core.Vertx vertx;
    
    // Reactor
    private Sinks.Many<Object> reactorSink;

    // Virtual Threads
    private ExecutorService virtualThreadExecutor;

    private static final io.github.namanoncode.zthread.event.CustomEvent EVENT = new io.github.namanoncode.zthread.event.CustomEvent("bench");

    // Consumer threads for raw queues
    private volatile boolean running;
    private ExecutorService queueConsumerPool;
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

        zRuntimes = new ZRuntime[CONSUMERS];
        for (int i = 0; i < CONSUMERS; i++) {
            zRuntimes[i] = ZRuntime.builder().build();
        }

        nettyEventLoopGroup = new DefaultEventLoopGroup(CONSUMERS);
        
        io.vertx.core.VertxOptions options = new io.vertx.core.VertxOptions().setEventLoopPoolSize(CONSUMERS);
        vertx = io.vertx.core.Vertx.vertx(options);
        
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        producerPool = Executors.newFixedThreadPool(PRODUCERS);
        queueConsumerPool = Executors.newFixedThreadPool(CONSUMERS);
    }

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException {
        for (ZRuntime zr : zRuntimes) {
            zr.shutdown();
            zr.awaitTermination(5, TimeUnit.SECONDS);
        }

        nettyEventLoopGroup.shutdownGracefully().await(5, TimeUnit.SECONDS);

        CountDownLatch vertxLatch = new CountDownLatch(1);
        vertx.close().onComplete(v -> vertxLatch.countDown());
        vertxLatch.await();

        virtualThreadExecutor.shutdown();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
        
        producerPool.shutdown();
        producerPool.awaitTermination(5, TimeUnit.SECONDS);

        queueConsumerPool.shutdown();
        queueConsumerPool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        queueLatch = new CountDownLatch(BATCH_SIZE);
        running = true;
    }

    @TearDown(Level.Invocation)
    public void teardownInvocation() {
        running = false;
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

    private void startBlockingQueueConsumers(BlockingQueue<Object> queue, Blackhole bh) {
        activeBlockingQueue = queue;
        for (int i = 0; i < CONSUMERS; i++) {
            queueConsumerPool.submit(() -> {
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
        }
    }

    private void startConcurrentQueueConsumers(ConcurrentLinkedQueue<Object> queue, Blackhole bh) {
        activeConcurrentQueue = queue;
        for (int i = 0; i < CONSUMERS; i++) {
            queueConsumerPool.submit(() -> {
                while (running) {
                    Object obj = activeConcurrentQueue.poll();
                    if (obj != null) {
                        bh.consume(obj);
                        queueLatch.countDown();
                    }
                }
            });
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchArrayBlockingQueue(Blackhole bh) throws InterruptedException {
        startBlockingQueueConsumers(arrayQueue, bh);
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
        startBlockingQueueConsumers(linkedQueue, bh);
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
    public void benchConcurrentLinkedQueue(Blackhole bh) throws InterruptedException {
        startConcurrentQueueConsumers(concurrentQueue, bh);
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
        EventHandler<io.github.namanoncode.zthread.event.CustomEvent> handler = evt -> {
            bh.consume(evt);
            latch.countDown();
        };
        for (ZRuntime zr : zRuntimes) {
            zr.on(io.github.namanoncode.zthread.event.CustomEvent.class, handler);
        }

        AtomicInteger index = new AtomicInteger();
        runProducers(() -> {
            int pIndex = index.getAndIncrement();
            ZRuntime target = zRuntimes[pIndex % CONSUMERS];
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                target.post(EVENT);
            }
        });
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchNetty(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                nettyEventLoopGroup.execute(() -> {
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
            .publishOn(Schedulers.parallel())
            .subscribe(evt -> {
                bh.consume(evt);
                latch.countDown();
            });

        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                reactorSink.tryEmitNext(EVENT);
            }
        });
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchVertx(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
        io.vertx.core.eventbus.MessageConsumer<Object> consumer = vertx.eventBus().localConsumer("benchmark.address", msg -> {
            bh.consume(msg.body());
            latch.countDown();
        });

        CountDownLatch regLatch = new CountDownLatch(1);
        consumer.completionHandler(res -> regLatch.countDown());
        regLatch.await();

        runProducers(() -> {
            for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                vertx.eventBus().send("benchmark.address", EVENT);
            }
        });
        latch.await();
        consumer.unregister();
    }
}
