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
 * Immutable snapshot of runtime metrics at a point in time.
 *
 * @param totalEventsProcessed total number of events dispatched to handlers
 * @param totalWakeups total number of eventfd wakeups
 * @param totalDroppedEvents total number of events dropped due to queue overflow
 * @param averageLoopLatencyNanos average event loop iteration latency in nanoseconds
 * @param maxLoopLatencyNanos maximum event loop iteration latency in nanoseconds
 * @param averageHandlerTimeNanos average handler execution time in nanoseconds
 * @param maxHandlerTimeNanos maximum handler execution time in nanoseconds
 * @param currentQueueDepth current number of items in the event queue
 */
public record MetricsSnapshot(
    long totalEventsProcessed,
    long totalWakeups,
    long totalDroppedEvents,
    long averageLoopLatencyNanos,
    long maxLoopLatencyNanos,
    long averageHandlerTimeNanos,
    long maxHandlerTimeNanos,
    int currentQueueDepth) {}
