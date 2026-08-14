package io.github.namanoncode.zthread.benchmark.adapters;

import io.github.namanoncode.zthread.benchmark.adapters.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.adapters.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingQueueAdapter implements EventRuntimeAdapter {

    private final BlockingQueue<BenchmarkEvent> queue;
    private final List<Thread> consumers = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BlockingQueueAdapter(BlockingQueue<BenchmarkEvent> queue) {
        this.queue = queue;
    }

    @Override
    public void start(EventHandler handler, int consumerCount) {
        running.set(true);
        for (int i = 0; i < consumerCount; i++) {
            Thread t = new Thread(() -> {
                while (running.get() || !queue.isEmpty()) {
                    try {
                        BenchmarkEvent event = queue.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (event != null) {
                            handler.onEvent(event);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            t.setName("BQ-Consumer-" + i);
            t.setDaemon(true);
            consumers.add(t);
            t.start();
        }
    }

    @Override
    public void submit(BenchmarkEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void shutdown() {
        running.set(false);
        for (Thread t : consumers) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
