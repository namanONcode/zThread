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

import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ZEventMonoTest {

  @Test
  void testMonoEmitsSingleEventAndCancels() {
    AtomicBoolean cancelled = new AtomicBoolean(false);
    final EventHandler[] capturedHandler = new EventHandler[1];

    StubZRuntime runtime = new StubZRuntime() {
      @Override
      @SuppressWarnings("unchecked")
      public <T extends ZEvent> HandlerRegistration on(Class<T> eventType, EventHandler<T> handler) {
        capturedHandler[0] = (EventHandler) handler;
        return new HandlerRegistration() {
          @Override public void cancel() { cancelled.set(true); }
          @Override public boolean isCancelled() { return cancelled.get(); }
        };
      }
    };

    Mono<CustomEvent> mono = ReactorBridge.onNextEvent(runtime, CustomEvent.class);

    StepVerifier.create(mono)
        .then(() -> {
          capturedHandler[0].handle(new CustomEvent("first"));
        })
        .expectNextMatches(e -> e.payload().equals("first"))
        .verifyComplete();

    // Verify it automatically cancelled the registration after the first event
    assertThat(cancelled.get()).isTrue();
  }
}
