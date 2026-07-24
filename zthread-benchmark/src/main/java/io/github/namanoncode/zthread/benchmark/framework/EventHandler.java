package io.github.namanoncode.zthread.benchmark.framework;

/**
 * Universal event handler interface. 
 * Prevents implementations from cheating by doing different work.
 */
public interface EventHandler {
    void onEvent(BenchmarkEvent event);
}
