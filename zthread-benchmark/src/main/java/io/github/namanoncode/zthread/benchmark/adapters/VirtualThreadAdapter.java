package io.github.namanoncode.zthread.benchmark.adapters;

import io.github.namanoncode.zthread.benchmark.adapters.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.adapters.EventHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadAdapter implements EventRuntimeAdapter {

    private ExecutorService executor;
    private EventHandler handler;

    @Override
    public void start(EventHandler handler, int consumers) {
        this.handler = handler;
        // Java 21+ virtual threads
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void submit(BenchmarkEvent event) {
        executor.execute(() -> handler.onEvent(event));
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
