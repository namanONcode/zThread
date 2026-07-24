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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lock-free implementation of {@link RuntimeMetrics}.
 *
 * <p>Uses {@link LongAdder} for high-contention counters and {@link AtomicLong}
 * for max tracking. Safe for concurrent access from multiple producer threads
 * and the event loop thread.
 *
 * <p>Thread safety: All methods are thread-safe.
 */
public final class DefaultRuntimeMetrics implements RuntimeMetrics {

  private final LongAdder eventsProcessed = new LongAdder();
  private final LongAdder wakeups = new LongAdder();
  private final LongAdder droppedEvents = new LongAdder();
  private final LongAdder totalLoopLatency = new LongAdder();
  private final LongAdder loopIterations = new LongAdder();
  private final AtomicLong maxLoopLatency = new AtomicLong(0);
  private final LongAdder totalHandlerTime = new LongAdder();
  private final LongAdder handlerInvocations = new LongAdder();
  private final AtomicLong maxHandlerTime = new AtomicLong(0);
  private final AtomicInteger queueDepth = new AtomicInteger(0);

  @Override
  public void recordLoopLatency(long nanos) {
    totalLoopLatency.add(nanos);
    loopIterations.increment();
    updateMax(maxLoopLatency, nanos);
  }

  @Override
  public void incrementEventsProcessed() {
    eventsProcessed.increment();
  }

  @Override
  public void incrementWakeups() {
    wakeups.increment();
  }

  @Override
  public void recordHandlerTime(long nanos) {
    totalHandlerTime.add(nanos);
    handlerInvocations.increment();
    updateMax(maxHandlerTime, nanos);
  }

  @Override
  public void updateQueueDepth(int depth) {
    queueDepth.set(depth);
  }

  @Override
  public void incrementDroppedEvents() {
    droppedEvents.increment();
  }

  @Override
  public MetricsSnapshot snapshot() {
    long iterations = loopIterations.sum();
    long invocations = handlerInvocations.sum();

    return new MetricsSnapshot(
        eventsProcessed.sum(),
        wakeups.sum(),
        droppedEvents.sum(),
        iterations > 0 ? totalLoopLatency.sum() / iterations : 0,
        maxLoopLatency.get(),
        invocations > 0 ? totalHandlerTime.sum() / invocations : 0,
        maxHandlerTime.get(),
        queueDepth.get());
  }

  @Override
  public void reset() {
    eventsProcessed.reset();
    wakeups.reset();
    droppedEvents.reset();
    totalLoopLatency.reset();
    loopIterations.reset();
    maxLoopLatency.set(0);
    totalHandlerTime.reset();
    handlerInvocations.reset();
    maxHandlerTime.set(0);
    queueDepth.set(0);
  }

  private static void updateMax(AtomicLong max, long value) {
    long current;
    do {
      current = max.get();
      if (value <= current) {
        return;
      }
    } while (!max.compareAndSet(current, value));
  }
}
