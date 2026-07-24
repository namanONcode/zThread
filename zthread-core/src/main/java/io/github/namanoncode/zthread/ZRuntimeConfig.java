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
package io.github.namanoncode.zthread;

/**
 * Immutable configuration for a {@link ZRuntime} instance.
 *
 * <p>Created by the {@link ZRuntimeBuilder} and passed to the runtime implementation.
 *
 * @param threadName the name of the event loop thread
 * @param bufferSize the MPSC ring buffer capacity (power of two)
 * @param maxEventsPerPoll the maximum events retrieved per epoll_wait call
 * @param metricsEnabled whether runtime metrics collection is enabled
 * @param debugEnabled whether debug logging is enabled
 */
public record ZRuntimeConfig(
    String threadName,
    int bufferSize,
    int maxEventsPerPoll,
    boolean metricsEnabled,
    boolean debugEnabled) {

  /**
   * Creates a new configuration with validation.
   *
   * @param threadName the thread name, must not be null
   * @param bufferSize the buffer size, must be positive
   * @param maxEventsPerPoll the max events per poll, must be positive
   * @param metricsEnabled whether metrics are enabled
   * @param debugEnabled whether debug is enabled
   */
  public ZRuntimeConfig {
    if (threadName == null || threadName.isBlank()) {
      throw new IllegalArgumentException("threadName must not be null or blank");
    }
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("bufferSize must be positive: " + bufferSize);
    }
    if (maxEventsPerPoll <= 0) {
      throw new IllegalArgumentException(
          "maxEventsPerPoll must be positive: " + maxEventsPerPoll);
    }
  }
}
