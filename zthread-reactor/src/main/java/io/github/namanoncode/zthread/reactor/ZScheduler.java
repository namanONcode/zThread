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
import java.util.concurrent.TimeUnit;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

/**
 * Reactor Scheduler backed by a zThread runtime.
 */
final class ZScheduler implements Scheduler {

  private final ZRuntime runtime;

  ZScheduler(ZRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public Disposable schedule(Runnable task) {
    HandlerRegistration reg = runtime.schedule(task, 0, TimeUnit.NANOSECONDS);
    return reg::cancel;
  }

  @Override
  public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
    HandlerRegistration reg = runtime.schedule(task, delay, unit);
    return reg::cancel;
  }

  @Override
  public Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
    HandlerRegistration reg = runtime.schedulePeriodic(task, initialDelay, period, unit);
    return reg::cancel;
  }

  @Override
  public Worker createWorker() {
    return new ZSchedulerWorker(runtime);
  }

  @Override
  public void dispose() {
    runtime.close();
  }

  @Override
  public boolean isDisposed() {
    return !runtime.isRunning();
  }
}
