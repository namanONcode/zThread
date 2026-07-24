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

import java.util.concurrent.TimeUnit;

/**
 * Scheduler for executing tasks on the event loop with timing control.
 *
 * <p>Tasks are backed by Linux timerfd file descriptors, providing kernel-level
 * timing precision without busy-waiting or polling.
 *
 * <p>Supported scheduling modes:
 * <ul>
 *   <li><b>Immediate:</b> Execute on the next event loop iteration</li>
 *   <li><b>Delayed:</b> Execute after a specified delay (one-shot)</li>
 *   <li><b>Periodic:</b> Execute repeatedly at a fixed rate</li>
 * </ul>
 *
 * <p>Thread safety: Scheduling methods are thread-safe and can be called from any thread.
 * Task execution always occurs on the event loop thread.
 */
public interface Scheduler extends AutoCloseable {

  /**
   * Schedules a task for immediate execution on the event loop.
   *
   * @param task the task to execute
   */
  void executeImmediate(Runnable task);

  /**
   * Schedules a one-shot task to execute after the given delay.
   *
   * @param task the task to execute
   * @param delay the delay before execution
   * @param unit the time unit
   * @return a handle for cancellation
   */
  CancellableTask scheduleDelayed(Runnable task, long delay, TimeUnit unit);

  /**
   * Schedules a periodic task with the given initial delay and period.
   *
   * @param task the task to execute
   * @param initialDelay the delay before the first execution
   * @param period the period between subsequent executions
   * @param unit the time unit
   * @return a handle for cancellation
   */
  CancellableTask schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit);

  /**
   * Returns the number of active scheduled tasks.
   *
   * @return the count of active tasks
   */
  int activeTaskCount();

  /**
   * Cancels all scheduled tasks and releases resources.
   */
  @Override
  void close();

  /**
   * A handle representing a scheduled task that can be cancelled.
   */
  interface CancellableTask {

    /**
     * Cancels the scheduled task. If the task is already executing, it will complete
     * but will not fire again.
     *
     * @return {@code true} if the task was successfully cancelled
     */
    boolean cancel();

    /**
     * Returns whether this task has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    boolean isCancelled();
  }
}
