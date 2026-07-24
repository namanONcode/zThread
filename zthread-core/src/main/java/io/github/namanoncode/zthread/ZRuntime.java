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

import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import io.github.namanoncode.zthread.metrics.RuntimeMetrics;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the zThread event runtime.
 *
 * <p>A {@code ZRuntime} manages a single event loop thread backed by Linux kernel event
 * mechanisms (epoll, eventfd, timerfd, signalfd, inotify). The runtime sleeps inside the
 * kernel when no work is available, achieving near-zero idle CPU utilization.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ZRuntime runtime = ZRuntime.builder()
 *     .threadName("my-event-loop")
 *     .bufferSize(8192)
 *     .metricsEnabled(true)
 *     .build();
 *
 * runtime.on(SocketEvent.class, event -> {
 *     // Handle socket events
 * });
 *
 * runtime.start();
 *
 * // Post custom events from any thread
 * runtime.post(new CustomEvent("hello"));
 *
 * // Schedule timers
 * runtime.schedule(() -> System.out.println("tick"), 1, TimeUnit.SECONDS);
 *
 * // Graceful shutdown
 * runtime.shutdown();
 * }</pre>
 *
 * <p>Thread safety: A {@code ZRuntime} is thread-safe. Events can be posted from any thread.
 * Handler registration should be done before calling {@link #start()}.
 *
 * @see ZRuntimeBuilder
 * @see EventLoop
 * @see EventDispatcher
 */
public interface ZRuntime extends AutoCloseable {

  /**
   * Creates a new {@code ZRuntime} with default configuration.
   *
   * @return a new runtime instance, not yet started
   */
  static ZRuntime create() {
    return builder().build();
  }

  /**
   * Creates a new {@link ZRuntimeBuilder} for configuring a {@code ZRuntime}.
   *
   * @return a new builder instance
   */
  static ZRuntimeBuilder builder() {
    return new ZRuntimeBuilder();
  }

  /**
   * Starts the event loop. The runtime will begin processing events on a dedicated thread.
   *
   * <p>This method is idempotent — calling it on an already-started runtime has no effect.
   *
   * @throws io.github.namanoncode.zthread.exception.EventLoopException if the event loop
   *     fails to start
   */
  void start();

  /**
   * Initiates a graceful shutdown of the runtime. The event loop will finish processing
   * currently queued events before stopping.
   *
   * <p>This method is non-blocking. Use {@link #awaitTermination(long, TimeUnit)} to wait
   * for completion.
   */
  void shutdown();

  /**
   * Waits for the runtime to terminate after a {@link #shutdown()} call.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return {@code true} if the runtime terminated, {@code false} if the timeout elapsed
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

  /**
   * Returns whether the runtime is currently running.
   *
   * @return {@code true} if the event loop is active
   */
  boolean isRunning();

  /**
   * Registers an event handler for the specified event type.
   *
   * <p>Multiple handlers can be registered for the same event type. Handlers are invoked
   * in registration order on the event loop thread.
   *
   * <p>No reflection is used. Dispatch is performed via type-safe generic matching.
   *
   * @param <T> the event type
   * @param eventType the class of events to handle
   * @param handler the handler to invoke when events of this type are dispatched
   * @return a registration handle that can be used to cancel the registration
   * @throws io.github.namanoncode.zthread.exception.RegistrationException if registration fails
   */
  <T extends ZEvent> HandlerRegistration on(Class<T> eventType, EventHandler<T> handler);

  /**
   * Posts a custom event to the event loop from any thread.
   *
   * <p>The event is enqueued in a lock-free ring buffer and the event loop is woken up
   * via an eventfd write. This is the primary mechanism for cross-thread communication.
   *
   * @param event the event to post
   * @throws io.github.namanoncode.zthread.exception.EventLoopException if the event
   *     cannot be enqueued (e.g., buffer full)
   */
  void post(CustomEvent event);

  /**
   * Attempts to post a custom event to the event loop without throwing an exception if full.
   *
   * @param event the event to post
   * @return {@code true} if successfully enqueued, {@code false} if ring buffer is full
   */
  boolean tryPost(CustomEvent event);

  /**
   * Schedules a one-shot task to execute after the given delay.
   *
   * <p>The task is backed by a Linux timerfd and will fire with kernel-level precision.
   *
   * @param task the task to execute on the event loop thread
   * @param delay the delay before execution
   * @param unit the time unit of the delay
   * @return a registration handle for cancellation
   */
  HandlerRegistration schedule(Runnable task, long delay, TimeUnit unit);

  /**
   * Schedules a periodic task with the given initial delay and period.
   *
   * @param task the task to execute on the event loop thread
   * @param initialDelay the delay before the first execution
   * @param period the period between subsequent executions
   * @param unit the time unit of the delay and period
   * @return a registration handle for cancellation
   */
  HandlerRegistration schedulePeriodic(
      Runnable task, long initialDelay, long period, TimeUnit unit);

  /**
   * Starts watching a file system path for changes using inotify.
   *
   * @param path the path to watch (file or directory)
   * @return a registration handle for cancellation
   * @throws io.github.namanoncode.zthread.exception.RegistrationException if the watch
   *     cannot be registered
   */
  HandlerRegistration watch(Path path);

  /**
   * Starts watching a file system path with a specific event mask.
   *
   * @param path the path to watch
   * @param mask the inotify event mask (e.g., IN_CREATE | IN_MODIFY)
   * @return a registration handle for cancellation
   */
  HandlerRegistration watch(Path path, int mask);

  /**
   * Returns the runtime metrics collector.
   *
   * @return the metrics instance, never null
   */
  RuntimeMetrics metrics();

  /**
   * Returns the runtime configuration.
   *
   * @return the immutable configuration, never null
   */
  ZRuntimeConfig config();

  /**
   * Closes the runtime, releasing all resources. Equivalent to {@link #shutdown()} followed
   * by waiting for termination.
   */
  @Override
  void close();
}
