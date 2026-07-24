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

import io.github.namanoncode.zthread.exception.ConfigurationException;
import io.github.namanoncode.zthread.util.Preconditions;
import java.util.ServiceLoader;

/**
 * Fluent builder for constructing a {@link ZRuntime} instance.
 *
 * <p>The builder uses {@link ServiceLoader} to discover the platform-specific runtime
 * implementation. On Linux, this will load the {@code LinuxRuntime} from the
 * {@code zthread-linux} module.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ZRuntime runtime = ZRuntime.builder()
 *     .threadName("event-loop")
 *     .bufferSize(4096)
 *     .maxEventsPerPoll(64)
 *     .metricsEnabled(true)
 *     .debugEnabled(false)
 *     .build();
 * }</pre>
 *
 * <p>Thread safety: This builder is not thread-safe. It should be used from a single thread.
 */
public final class ZRuntimeBuilder {

  private String threadName = "zthread-event-loop";
  private int bufferSize = 4096;
  private int maxEventsPerPoll = 64;
  private boolean metricsEnabled = true;
  private boolean debugEnabled = false;

  /** Creates a new builder with default settings. */
  ZRuntimeBuilder() {}

  /**
   * Sets the name of the event loop thread.
   *
   * @param threadName the thread name, must not be null or blank
   * @return this builder
   */
  public ZRuntimeBuilder threadName(String threadName) {
    Preconditions.requireNonBlank(threadName, "threadName");
    this.threadName = threadName;
    return this;
  }

  /**
   * Sets the ring buffer capacity for the MPSC event queue.
   *
   * <p>The value will be rounded up to the nearest power of two. Minimum value is 64.
   *
   * @param bufferSize the buffer capacity
   * @return this builder
   */
  public ZRuntimeBuilder bufferSize(int bufferSize) {
    Preconditions.requirePositive(bufferSize, "bufferSize");
    this.bufferSize = Math.max(64, bufferSize);
    return this;
  }

  /**
   * Sets the maximum number of events to retrieve per epoll_wait call.
   *
   * @param maxEventsPerPoll the maximum events per poll, must be positive
   * @return this builder
   */
  public ZRuntimeBuilder maxEventsPerPoll(int maxEventsPerPoll) {
    Preconditions.requirePositive(maxEventsPerPoll, "maxEventsPerPoll");
    this.maxEventsPerPoll = maxEventsPerPoll;
    return this;
  }

  /**
   * Enables or disables runtime metrics collection.
   *
   * @param enabled true to enable metrics
   * @return this builder
   */
  public ZRuntimeBuilder metricsEnabled(boolean enabled) {
    this.metricsEnabled = enabled;
    return this;
  }

  /**
   * Enables or disables debug logging.
   *
   * @param enabled true to enable debug logging
   * @return this builder
   */
  public ZRuntimeBuilder debugEnabled(boolean enabled) {
    this.debugEnabled = enabled;
    return this;
  }

  /**
   * Builds and returns a new {@link ZRuntime} instance.
   *
   * <p>The runtime implementation is discovered via {@link ServiceLoader}. The
   * {@code zthread-linux} module must be on the classpath for Linux systems.
   *
   * @return a new, unconfigured runtime
   * @throws ConfigurationException if no runtime implementation is found
   */
  public ZRuntime build() {
    ZRuntimeConfig config =
        new ZRuntimeConfig(threadName, bufferSize, maxEventsPerPoll, metricsEnabled, debugEnabled);

    ServiceLoader<ZRuntimeFactory> loader = ServiceLoader.load(ZRuntimeFactory.class);
    ZRuntimeFactory factory =
        loader
            .findFirst()
            .orElseThrow(
                () ->
                    new ConfigurationException(
                        "No ZRuntime implementation found. "
                            + "Ensure zthread-linux is on the classpath."));

    return factory.create(config);
  }

  /**
   * Returns the configured thread name.
   *
   * @return the thread name
   */
  public String threadName() {
    return threadName;
  }

  /**
   * Returns the configured buffer size.
   *
   * @return the buffer size
   */
  public int bufferSize() {
    return bufferSize;
  }

  /**
   * Returns the configured max events per poll.
   *
   * @return the max events per poll
   */
  public int maxEventsPerPoll() {
    return maxEventsPerPoll;
  }

  /**
   * Returns whether metrics are enabled.
   *
   * @return true if metrics are enabled
   */
  public boolean metricsEnabled() {
    return metricsEnabled;
  }

  /**
   * Returns whether debug mode is enabled.
   *
   * @return true if debug is enabled
   */
  public boolean debugEnabled() {
    return debugEnabled;
  }
}
