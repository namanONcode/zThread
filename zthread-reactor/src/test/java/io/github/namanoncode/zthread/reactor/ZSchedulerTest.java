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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.namanoncode.zthread.handler.HandlerRegistration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

class ZSchedulerTest {

  @Test
  void testScheduleImmediate() {
    AtomicBoolean cancelled = new AtomicBoolean(false);
    AtomicBoolean scheduled = new AtomicBoolean(false);

    StubZRuntime runtime = new StubZRuntime() {
      @Override
      public HandlerRegistration schedule(Runnable task, long delay, TimeUnit unit) {
        scheduled.set(true);
        return new HandlerRegistration() {
          @Override public void cancel() { cancelled.set(true); }
          @Override public boolean isCancelled() { return cancelled.get(); }
        };
      }
    };

    Scheduler scheduler = ReactorBridge.scheduler(runtime);
    Runnable task = () -> {};
    
    Disposable disposable = scheduler.schedule(task);
    assertThat(scheduled.get()).isTrue();
    
    disposable.dispose();
    assertThat(cancelled.get()).isTrue();
  }

  @Test
  void testWorkerDisposalCancelsTasks() {
    AtomicBoolean cancel1 = new AtomicBoolean(false);
    AtomicBoolean cancel2 = new AtomicBoolean(false);
    
    StubZRuntime runtime = new StubZRuntime() {
      int count = 0;
      @Override
      public HandlerRegistration schedule(Runnable task, long delay, TimeUnit unit) {
        if (count++ == 0) {
          return new HandlerRegistration() {
            @Override public void cancel() { cancel1.set(true); }
            @Override public boolean isCancelled() { return cancel1.get(); }
          };
        } else {
          return new HandlerRegistration() {
            @Override public void cancel() { cancel2.set(true); }
            @Override public boolean isCancelled() { return cancel2.get(); }
          };
        }
      }
    };

    Scheduler scheduler = ReactorBridge.scheduler(runtime);
    Scheduler.Worker worker = scheduler.createWorker();
    
    worker.schedule(() -> {});
    worker.schedule(() -> {});
    
    assertThat(worker.isDisposed()).isFalse();
    worker.dispose();
    assertThat(worker.isDisposed()).isTrue();
    
    assertThat(cancel1.get()).isTrue();
    assertThat(cancel2.get()).isTrue();
  }
}
