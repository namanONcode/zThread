package io.github.namanoncode.zthread.benchmark.framework.adapter;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.ZRuntimeBuilder;
import io.github.namanoncode.zthread.benchmark.framework.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.framework.EventHandler;
import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.ZEvent;

public class ZThreadAdapter implements EventRuntimeAdapter {

    private ZRuntime runtime;

    @Override
    public void start(EventHandler handler, int consumers) {
        // ZRuntime is inherently single-threaded per event loop, so max threads implies multiple event loops
        // but for standard adapter we use 1.
        runtime = ZRuntime.builder()
                .threadName("zthread-benchmark")
                .bufferSize(32768)
                .build();
        
        runtime.on(CustomEvent.class, (CustomEvent e) -> {
            handler.onEvent((BenchmarkEvent) e.payload());
        });
        
        runtime.start();
    }

    @Override
    public void submit(BenchmarkEvent event) {
        runtime.post(new CustomEvent(event));
    }

    @Override
    public void shutdown() {
        if (runtime != null) {
            runtime.shutdown();
            try {
                runtime.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
