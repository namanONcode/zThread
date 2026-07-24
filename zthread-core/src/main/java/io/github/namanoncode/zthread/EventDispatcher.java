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

import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.github.namanoncode.zthread.handler.HandlerRegistration;

/**
 * Type-safe event dispatcher that manages handler registration and event dispatch.
 *
 * <p>The dispatcher maps event classes to registered handlers and invokes them when
 * events arrive. No reflection is used — dispatch is performed via direct type matching
 * on the event's class.
 *
 * <p>Thread safety: Registration methods are thread-safe. Dispatch must only be called
 * from the event loop thread.
 *
 * @see EventHandler
 * @see HandlerRegistration
 */
public interface EventDispatcher {

  /**
   * Registers a handler for the specified event type.
   *
   * @param <T> the event type
   * @param eventType the class of events to handle
   * @param handler the handler to invoke
   * @return a registration handle for cancellation
   */
  <T extends ZEvent> HandlerRegistration register(Class<T> eventType, EventHandler<T> handler);

  /**
   * Dispatches an event to all registered handlers for its type.
   *
   * <p>Handlers are invoked in registration order. If a handler throws an exception,
   * it is caught and logged, and remaining handlers are still invoked.
   *
   * @param event the event to dispatch
   */
  void dispatch(ZEvent event);

  /**
   * Returns the number of handlers registered for the given event type.
   *
   * @param eventType the event class
   * @return the number of registered handlers
   */
  int handlerCount(Class<? extends ZEvent> eventType);

  /**
   * Removes all registered handlers.
   */
  void clear();
}
