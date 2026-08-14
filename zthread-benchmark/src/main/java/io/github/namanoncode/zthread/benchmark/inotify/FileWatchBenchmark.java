package io.github.namanoncode.zthread.benchmark.inotify;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.event.FileEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 2)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class FileWatchBenchmark {

    public static final int BATCH_SIZE = 1_000;

    private Path tempDir;
    
    // zThread
    private ZRuntime zRuntime;
    
    // Java NIO WatchService
    private WatchService watchService;
    private ExecutorService nioExecutor;
    private volatile boolean running;

    private CountDownLatch latch;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("zthread-bench-filewatch");
        
        // Setup zThread
        zRuntime = ZRuntime.builder().build();
        zRuntime.start();
        zRuntime.watch(tempDir);
        
        // Setup NIO WatchService
        watchService = FileSystems.getDefault().newWatchService();
        tempDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        
        nioExecutor = Executors.newSingleThreadExecutor();
        running = true;
        
        nioExecutor.submit(() -> {
            try {
                while (running) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && latch != null) {
                            latch.countDown();
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                // ignore
            }
        });
    }

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException, IOException {
        zRuntime.shutdown();
        zRuntime.awaitTermination(5, TimeUnit.SECONDS);

        running = false;
        watchService.close();
        nioExecutor.shutdownNow();
        nioExecutor.awaitTermination(5, TimeUnit.SECONDS);
        
        // Clean up temp dir
        File[] files = tempDir.toFile().listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        Files.deleteIfExists(tempDir);
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchJavaNioWatchService(Blackhole bh) throws InterruptedException, IOException {
        latch = new CountDownLatch(BATCH_SIZE);
        
        for (int i = 0; i < BATCH_SIZE; i++) {
            Files.createFile(tempDir.resolve("nio-" + i + ".tmp"));
        }
        
        latch.await();
        
        // Cleanup for next invocation
        for (int i = 0; i < BATCH_SIZE; i++) {
            Files.deleteIfExists(tempDir.resolve("nio-" + i + ".tmp"));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchZThreadInotify(Blackhole bh) throws InterruptedException, IOException {
        latch = new CountDownLatch(BATCH_SIZE);
        io.github.namanoncode.zthread.handler.HandlerRegistration reg = zRuntime.on(FileEvent.class, evt -> {
            bh.consume(evt);
            latch.countDown();
        });

        for (int i = 0; i < BATCH_SIZE; i++) {
            Files.createFile(tempDir.resolve("zthread-" + i + ".tmp"));
        }

        latch.await();
        reg.cancel();

        // Cleanup for next invocation
        for (int i = 0; i < BATCH_SIZE; i++) {
            Files.deleteIfExists(tempDir.resolve("zthread-" + i + ".tmp"));
        }
    }
}
