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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.ErrorEvent;
import io.github.namanoncode.zthread.event.SocketEvent;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import io.github.namanoncode.zthread.metrics.DefaultRuntimeMetrics;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultEventDispatcherTest {

  private DefaultEventDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    dispatcher = new DefaultEventDispatcher(new DefaultRuntimeMetrics());
  }

  @Test
  void dispatchInvokesRegisteredHandler() {
    AtomicReference<CustomEvent> received = new AtomicReference<>();
    dispatcher.register(CustomEvent.class, received::set);

    CustomEvent event = new CustomEvent("test-payload");
    dispatcher.dispatch(event);

    assertThat(received.get()).isNotNull();
    assertThat(received.get().payload()).isEqualTo("test-payload");
  }

  @Test
  void dispatchInvokesMultipleHandlers() {
    AtomicInteger count = new AtomicInteger();
    dispatcher.register(CustomEvent.class, e -> count.incrementAndGet());
    dispatcher.register(CustomEvent.class, e -> count.incrementAndGet());
    dispatcher.register(CustomEvent.class, e -> count.incrementAndGet());

    dispatcher.dispatch(new CustomEvent("test"));

    assertThat(count.get()).isEqualTo(3);
  }

  @Test
  void cancelledHandlerIsNotInvoked() {
    AtomicInteger count = new AtomicInteger();
    HandlerRegistration reg = dispatcher.register(CustomEvent.class, e -> count.incrementAndGet());

    reg.cancel();
    dispatcher.dispatch(new CustomEvent("test"));

    assertThat(count.get()).isZero();
    assertThat(reg.isCancelled()).isTrue();
  }

  @Test
  void cancelIsIdempotent() {
    HandlerRegistration reg = dispatcher.register(CustomEvent.class, e -> {});
    reg.cancel();
    reg.cancel();
    assertThat(reg.isCancelled()).isTrue();
  }

  @Test
  void dispatchToWrongTypeDoesNothing() {
    AtomicInteger count = new AtomicInteger();
    dispatcher.register(SocketEvent.class, e -> count.incrementAndGet());

    dispatcher.dispatch(new CustomEvent("test"));

    assertThat(count.get()).isZero();
  }

  @Test
  void handlerExceptionDoesNotStopOtherHandlers() {
    AtomicInteger count = new AtomicInteger();
    dispatcher.register(CustomEvent.class, e -> {
      throw new RuntimeException("test error");
    });
    dispatcher.register(CustomEvent.class, e -> count.incrementAndGet());

    dispatcher.dispatch(new CustomEvent("test"));

    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  void handlerCountReflectsRegistrations() {
    dispatcher.register(CustomEvent.class, e -> {});
    dispatcher.register(CustomEvent.class, e -> {});

    assertThat(dispatcher.handlerCount(CustomEvent.class)).isEqualTo(2);
    assertThat(dispatcher.handlerCount(SocketEvent.class)).isZero();
  }

  @Test
  void clearRemovesAllHandlers() {
    dispatcher.register(CustomEvent.class, e -> {});
    dispatcher.register(SocketEvent.class, e -> {});

    dispatcher.clear();

    assertThat(dispatcher.handlerCount(CustomEvent.class)).isZero();
    assertThat(dispatcher.handlerCount(SocketEvent.class)).isZero();
  }

  @Test
  void errorHandlerReceivesErrors() {
    AtomicReference<ErrorEvent> errorRef = new AtomicReference<>();
    dispatcher.register(ErrorEvent.class, errorRef::set);
    dispatcher.register(CustomEvent.class, e -> {
      throw new RuntimeException("boom");
    });

    dispatcher.dispatch(new CustomEvent("test"));

    assertThat(errorRef.get()).isNotNull();
    assertThat(errorRef.get().message()).contains("Handler exception");
    assertThat(errorRef.get().cause()).isInstanceOf(RuntimeException.class);
  }
}
