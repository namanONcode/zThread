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
import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import java.util.function.Consumer;
import reactor.core.publisher.MonoSink;

/**
 * Bridges zThread events into a Reactor Mono sink.
 * Completes the mono and cancels registration upon receiving the first event.
 *
 * @param <T> the event type
 */
final class ZEventMono<T extends ZEvent> implements Consumer<MonoSink<T>> {

  private final ZRuntime runtime;
  private final Class<T> eventType;

  ZEventMono(ZRuntime runtime, Class<T> eventType) {
    this.runtime = runtime;
    this.eventType = eventType;
  }

  @Override
  public void accept(MonoSink<T> sink) {
    // We use a final array to allow mutation from the inner lambda
    final HandlerRegistration[] reg = new HandlerRegistration[1];
    
    reg[0] = runtime.on(eventType, event -> {
      if (reg[0] != null) {
        reg[0].cancel();
      }
      sink.success(event);
    });

    sink.onCancel(() -> {
      if (reg[0] != null) {
        reg[0].cancel();
      }
    });
    sink.onDispose(() -> {
      if (reg[0] != null) {
        reg[0].cancel();
      }
    });
  }
}
