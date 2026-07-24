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
 * Interface for the event loop that drives the runtime.
 *
 * <p>The event loop is the core execution engine. It blocks in the kernel (via epoll_wait)
 * until events arrive, then dispatches them to registered handlers. The loop runs on a
 * single dedicated thread.
 *
 * <p>Thread safety: The {@link #start()} and {@link #stop()} methods are thread-safe.
 * The event processing itself occurs on the event loop thread.
 */
public interface EventLoop extends AutoCloseable {

  /**
   * Starts the event loop on a dedicated thread.
   *
   * <p>The loop will block inside the kernel when no events are pending, achieving
   * near-zero idle CPU utilization.
   */
  void start();

  /**
   * Signals the event loop to stop after draining remaining events.
   *
   * <p>This is non-blocking. The loop will complete its current iteration and then exit.
   */
  void stop();

  /**
   * Returns whether the event loop is currently running.
   *
   * @return {@code true} if the loop is active
   */
  boolean isRunning();

  /**
   * Wakes up the event loop if it is blocked in epoll_wait.
   *
   * <p>This is used by producer threads after enqueuing events in the ring buffer.
   * The wakeup is achieved by writing to an eventfd.
   */
  void wakeup();

  /**
   * Closes the event loop and releases all native resources (file descriptors, memory).
   */
  @Override
  void close();
}
