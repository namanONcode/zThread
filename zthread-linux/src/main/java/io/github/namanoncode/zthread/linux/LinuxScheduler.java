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
package io.github.namanoncode.zthread.linux;

import io.github.namanoncode.zthread.Scheduler;
import io.github.namanoncode.zthread.exception.NativeException;
import io.github.namanoncode.zthread.linux.native_.LinuxConstants;
import io.github.namanoncode.zthread.linux.native_.LinuxSyscalls;
import io.github.namanoncode.zthread.linux.native_.StructLayouts;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux timerfd-based implementation of {@link Scheduler}.
 */
@SuppressWarnings("PMD")
public final class LinuxScheduler implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(LinuxScheduler.class);
  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  private final LinuxEventLoop eventLoop;
  private final ConcurrentHashMap<Integer, TimerTask> activeTasks = new ConcurrentHashMap<>();
  private final Arena arena;

  public LinuxScheduler(LinuxEventLoop eventLoop) {
    this.eventLoop = eventLoop;
    this.arena = Arena.ofShared();
  }

  @Override
  public void executeImmediate(Runnable task) {
    // Immediate execution uses the event loop's ring buffer if possible,
    // but the MpscRingBuffer only takes longs. We'll use a timerfd with 1ns delay as a fallback,
    // or register a special callback. Since ZRuntime custom events exist, we can post a custom event
    // containing the Runnable.
    // Wait, ZRuntime custom event dispatching happens at the dispatcher level.
    // Let's use a 1ns timerfd for simplicity and unified lifecycle.
    scheduleDelayed(task, 1, TimeUnit.NANOSECONDS);
  }

  @Override
  public CancellableTask scheduleDelayed(Runnable task, long delay, TimeUnit unit) {
    long delayNanos = unit.toNanos(delay);
    if (delayNanos <= 0) {
      delayNanos = 1;
    }
    return scheduleInternal(task, delayNanos, 0);
  }

  @Override
  public CancellableTask schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
    long initialDelayNanos = unit.toNanos(initialDelay);
    if (initialDelayNanos <= 0) {
      initialDelayNanos = 1;
    }
    long periodNanos = unit.toNanos(period);
    if (periodNanos <= 0) {
      throw new IllegalArgumentException("Period must be > 0");
    }
    return scheduleInternal(task, initialDelayNanos, periodNanos);
  }

  @Override
  public int activeTaskCount() {
    return activeTasks.size();
  }

  @Override
  public void close() {
    for (TimerTask task : activeTasks.values()) {
      task.cancel();
    }
    activeTasks.clear();
    arena.close();
  }

  private CancellableTask scheduleInternal(Runnable task, long initialDelayNanos, long periodNanos) {
    int timerFd;
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      timerFd = (int) LinuxSyscalls.TIMERFD_CREATE.invokeExact(
          captureState, LinuxConstants.CLOCK_MONOTONIC,
          LinuxConstants.TFD_NONBLOCK | LinuxConstants.TFD_CLOEXEC);
      if (timerFd < 0) {
        throw new NativeException("timerfd_create failed",
            LinuxSyscalls.extractErrno(captureState));
      }
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("timerfd_create invocation failed", -1, e);
    }

    TimerTask timerTask = new TimerTask(timerFd, task, periodNanos == 0);
    activeTasks.put(timerFd, timerTask);

    long initialSec = initialDelayNanos / NANOS_PER_SECOND;
    long initialNsec = initialDelayNanos % NANOS_PER_SECOND;
    long periodSec = periodNanos / NANOS_PER_SECOND;
    long periodNsec = periodNanos % NANOS_PER_SECOND;

    try {
      MemorySegment itimerspec = arena.allocate(
          StructLayouts.ITIMERSPEC_SIZE, StructLayouts.ITIMERSPEC.byteAlignment());
      StructLayouts.writeItimerspec(itimerspec, periodSec, periodNsec, initialSec, initialNsec);

      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int result = (int) LinuxSyscalls.TIMERFD_SETTIME.invokeExact(
          captureState, timerFd, 0, itimerspec, MemorySegment.NULL);
      if (result < 0) {
        int errno = LinuxSyscalls.extractErrno(captureState);
        closeFd(timerFd);
        activeTasks.remove(timerFd);
        throw new NativeException("timerfd_settime failed", errno);
      }
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      closeFd(timerFd);
      activeTasks.remove(timerFd);
      throw new NativeException("timerfd_settime invocation failed", -1, e);
    }

    eventLoop.registerTimer(timerFd, timerTask);
    return timerTask;
  }

  private void closeFd(int fd) {
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int ignored = (int) LinuxSyscalls.CLOSE.invokeExact(captureState, fd);
    } catch (Throwable e) {
      LOG.error("Failed to close timerfd={}", fd, e);
    }
  }

  private class TimerTask implements CancellableTask, Runnable {
    private final int timerFd;
    private final Runnable target;
    private final boolean oneShot;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    TimerTask(int timerFd, Runnable target, boolean oneShot) {
      this.timerFd = timerFd;
      this.target = target;
      this.oneShot = oneShot;
    }

    @Override
    public boolean cancel() {
      if (cancelled.compareAndSet(false, true)) {
        eventLoop.unregisterTimer(timerFd);
        activeTasks.remove(timerFd);
        closeFd(timerFd);
        return true;
      }
      return false;
    }

    @Override
    public boolean isCancelled() {
      return cancelled.get();
    }

    @Override
    public void run() {
      if (cancelled.get()) {
        return;
      }
      try {
        target.run();
      } finally {
        if (oneShot) {
          cancel();
        }
      }
    }
  }
}
