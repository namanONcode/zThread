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

import io.github.namanoncode.zthread.DefaultEventDispatcher;
import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.ZRuntimeConfig;
import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import io.github.namanoncode.zthread.metrics.DefaultRuntimeMetrics;
import io.github.namanoncode.zthread.metrics.RuntimeMetrics;
import io.github.namanoncode.zthread.util.Preconditions;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Linux implementation of {@link ZRuntime}.
 */
public final class LinuxRuntime implements ZRuntime {

  private final ZRuntimeConfig config;
  private final DefaultRuntimeMetrics metrics;
  private final DefaultEventDispatcher dispatcher;
  private final LinuxEventLoop eventLoop;
  private final LinuxScheduler scheduler;
  private final LinuxInotifyWatcher inotifyWatcher;

  public LinuxRuntime(ZRuntimeConfig config) {
    this.config = Preconditions.requireNonNull(config, "config");
    this.metrics = new DefaultRuntimeMetrics();
    this.dispatcher = new DefaultEventDispatcher(metrics);
    this.eventLoop = new LinuxEventLoop(config, dispatcher, metrics);
    this.scheduler = new LinuxScheduler(eventLoop);
    this.inotifyWatcher = new LinuxInotifyWatcher(eventLoop, dispatcher);
  }

  @Override
  public void start() {
    eventLoop.start();
  }

  @Override
  public void shutdown() {
    eventLoop.stop();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    long end = System.nanoTime() + nanos;
    while (eventLoop.isRunning()) {
      if (System.nanoTime() > end) {
        return false;
      }
      Thread.sleep(10);
    }
    return true;
  }

  @Override
  public boolean isRunning() {
    return eventLoop.isRunning();
  }

  @Override
  public <T extends ZEvent> HandlerRegistration on(Class<T> eventType, EventHandler<T> handler) {
    return dispatcher.register(eventType, handler);
  }

  @Override
  public void post(CustomEvent event) {
    Preconditions.requireNonNull(event, "event");
    if (!eventLoop.postToRingBuffer(event)) {
      throw new io.github.namanoncode.zthread.exception.EventLoopException("Event ring buffer is full");
    }
  }

  @Override
  public boolean tryPost(CustomEvent event) {
    Preconditions.requireNonNull(event, "event");
    return eventLoop.postToRingBuffer(event);
  }

  @Override
  public HandlerRegistration schedule(Runnable task, long delay, TimeUnit unit) {
    final io.github.namanoncode.zthread.Scheduler.CancellableTask timerTask =
        scheduler.scheduleDelayed(task, delay, unit);
    return new HandlerRegistration() {
      @Override
      public void cancel() {
        timerTask.cancel();
      }

      @Override
      public boolean isCancelled() {
        return timerTask.isCancelled();
      }
    };
  }

  @Override
  public HandlerRegistration schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
    final io.github.namanoncode.zthread.Scheduler.CancellableTask timerTask =
        scheduler.schedulePeriodic(task, initialDelay, period, unit);
    return new HandlerRegistration() {
      @Override
      public void cancel() {
        timerTask.cancel();
      }

      @Override
      public boolean isCancelled() {
        return timerTask.isCancelled();
      }
    };
  }

  @Override
  public HandlerRegistration watch(Path path) {
    return inotifyWatcher.watch(path);
  }

  @Override
  public HandlerRegistration watch(Path path, int mask) {
    return inotifyWatcher.watch(path, mask);
  }

  @Override
  public RuntimeMetrics metrics() {
    return metrics;
  }

  @Override
  public ZRuntimeConfig config() {
    return config;
  }

  @Override
  public void close() {
    shutdown();
    try {
      awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    scheduler.close();
    inotifyWatcher.close();
    eventLoop.close();
  }
}
