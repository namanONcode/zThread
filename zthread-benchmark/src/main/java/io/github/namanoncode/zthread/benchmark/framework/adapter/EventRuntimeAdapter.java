package io.github.namanoncode.zthread.benchmark.framework.adapter;

import io.github.namanoncode.zthread.benchmark.framework.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.framework.EventHandler;

public interface EventRuntimeAdapter {

    /**
     * Initializes and starts the runtime.
     * @param handler The handler that must process every submitted event.
     * @param consumers The number of consumer threads to use (if applicable).
     */
    void start(EventHandler handler, int consumers);

    /**
     * Submits an event to the runtime.
     */
    void submit(BenchmarkEvent event);

    /**
     * Gracefully shuts down the runtime and blocks until completion.
     */
    void shutdown();
}
