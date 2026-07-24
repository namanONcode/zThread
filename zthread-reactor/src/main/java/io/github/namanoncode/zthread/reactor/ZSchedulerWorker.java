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
package io.github.namanoncode.zthread.reactor;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Scheduler;

/**
 * Worker implementation for {@link ZScheduler}.
 * A worker guarantees that all scheduled tasks are executed sequentially
 * and can be cancelled together. Since zThread has a single event loop thread,
 * sequential execution is guaranteed by default.
 */
final class ZSchedulerWorker implements Scheduler.Worker {

  private final ZRuntime runtime;
  private final AtomicBoolean disposed = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<HandlerRegistration> registrations = new ConcurrentLinkedQueue<>();

  ZSchedulerWorker(ZRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public Disposable schedule(Runnable task) {
    if (disposed.get()) {
      return Disposables.disposed();
    }
    HandlerRegistration reg = runtime.schedule(task, 0, TimeUnit.NANOSECONDS);
    registrations.add(reg);
    return reg::cancel;
  }

  @Override
  public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
    if (disposed.get()) {
      return Disposables.disposed();
    }
    HandlerRegistration reg = runtime.schedule(task, delay, unit);
    registrations.add(reg);
    return reg::cancel;
  }

  @Override
  public Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
    if (disposed.get()) {
      return Disposables.disposed();
    }
    HandlerRegistration reg = runtime.schedulePeriodic(task, initialDelay, period, unit);
    registrations.add(reg);
    return reg::cancel;
  }

  @Override
  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      HandlerRegistration reg = registrations.poll();
      while (reg != null) {
        reg.cancel();
        reg = registrations.poll();
      }
    }
  }

  @Override
  public boolean isDisposed() {
    return disposed.get();
  }
}
