package io.github.namanoncode.zthread.benchmark.adapters;

/**
 * Universal event handler interface. 
 * Prevents implementations from cheating by doing different work.
 */
public interface EventHandler {
    void onEvent(BenchmarkEvent event);
}
