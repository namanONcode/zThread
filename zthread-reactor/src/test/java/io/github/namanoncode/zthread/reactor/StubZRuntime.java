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
import io.github.namanoncode.zthread.ZRuntimeConfig;
import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import io.github.namanoncode.zthread.metrics.RuntimeMetrics;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

class StubZRuntime implements ZRuntime {

  @Override
  public void start() {}



  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public <T extends ZEvent> HandlerRegistration on(Class<T> eventType, EventHandler<T> handler) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HandlerRegistration schedule(Runnable task, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HandlerRegistration schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HandlerRegistration watch(Path path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HandlerRegistration watch(Path path, int mask) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void post(CustomEvent event) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryPost(CustomEvent event) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RuntimeMetrics metrics() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ZRuntimeConfig config() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {}

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return true;
  }

  @Override
  public void close() {}
}
