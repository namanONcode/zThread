package io.github.namanoncode.zthread.benchmark.framework.adapter;

import io.github.namanoncode.zthread.benchmark.framework.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.framework.EventHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorAdapter implements EventRuntimeAdapter {

    private final ExecutorService executor;
    private EventHandler handler;

    public ExecutorAdapter(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void start(EventHandler handler, int consumers) {
        this.handler = handler;
        // Consumer count is ignored as the executor pool size dictates it
    }

    @Override
    public void submit(BenchmarkEvent event) {
        executor.execute(() -> handler.onEvent(event));
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
