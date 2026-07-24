/*
 * Copyright (c) 2026 Naman Jain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project: zThread
 * Author: Naman Jain
 * GitHub: https://github.com/namanoncode/zThread
 */
package io.github.namanoncode.zthread.metrics;

/**
 * Interface for runtime metrics collection.
 *
 * <p>Provides counters and gauges for monitoring event loop performance.
 * The implementation uses lock-free atomic operations suitable for
 * concurrent access from producer threads and the event loop.
 *
 * @see MetricsSnapshot
 */
public interface RuntimeMetrics {

  /**
   * Records a single event loop iteration latency.
   *
   * @param nanos the iteration duration in nanoseconds
   */
  void recordLoopLatency(long nanos);

  /**
   * Increments the total events processed counter.
   */
  void incrementEventsProcessed();

  /**
   * Increments the wakeup counter (eventfd writes).
   */
  void incrementWakeups();

  /**
   * Records handler execution time.
   *
   * @param nanos the handler execution duration in nanoseconds
   */
  void recordHandlerTime(long nanos);

  /**
   * Updates the current queue depth.
   *
   * @param depth the current number of items in the event queue
   */
  void updateQueueDepth(int depth);

  /**
   * Increments the dropped events counter.
   */
  void incrementDroppedEvents();

  /**
   * Returns an immutable snapshot of the current metrics.
   *
   * @return a metrics snapshot
   */
  MetricsSnapshot snapshot();

  /**
   * Resets all counters to zero.
   */
  void reset();
}
