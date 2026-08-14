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

import io.github.namanoncode.zthread.event.ErrorEvent;
import io.github.namanoncode.zthread.event.ZEvent;
import io.github.namanoncode.zthread.handler.EventHandler;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import io.github.namanoncode.zthread.metrics.RuntimeMetrics;
import io.github.namanoncode.zthread.util.ObjectPool;
import io.github.namanoncode.zthread.util.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link EventDispatcher}.
 *
 * <p>Uses a {@link ConcurrentHashMap} of event class → handler list for registration.
 * Handler lists use {@link CopyOnWriteArrayList} for safe concurrent registration
 * while dispatch iterates without locking.
 *
 * <p>Thread safety: Registration is thread-safe. Dispatch must only be called
 * from the event loop thread.
 */
public final class DefaultEventDispatcher implements EventDispatcher {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEventDispatcher.class);

  private final Map<Class<? extends ZEvent>, List<HandlerEntry<?>>> handlers =
      new ConcurrentHashMap<>();
  private final RuntimeMetrics metrics;
  private final ObjectPool<ErrorEvent> errorEventPool;

  /**
   * Creates a new dispatcher.
   *
   * @param metrics the metrics collector
   */
  public DefaultEventDispatcher(RuntimeMetrics metrics) {
    this.metrics = Preconditions.requireNonNull(metrics, "metrics");
    this.errorEventPool = new ObjectPool<>(16, ErrorEvent::new);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ZEvent> HandlerRegistration register(
      Class<T> eventType, EventHandler<T> handler) {
    Preconditions.requireNonNull(eventType, "eventType");
    Preconditions.requireNonNull(handler, "handler");

    HandlerEntry<T> entry = new HandlerEntry<>(handler);
    handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(entry);

    LOG.debug("Registered handler for {}", eventType.getSimpleName());
    return entry;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes", "PMD.AvoidCatchingGenericException"})
  public void dispatch(ZEvent event) {
    List<HandlerEntry<?>> entries = handlers.get(event.getClass());
    if (entries == null || entries.isEmpty()) {
      return;
    }

    for (HandlerEntry entry : entries) {
      if (entry.isCancelled()) {
        continue;
      }
      long start = System.nanoTime();
      try {
        entry.handler().handle(event);
      } catch (Exception e) {
        LOG.error("Handler threw exception for {}: {}", event.getClass().getSimpleName(), e);
        dispatchError(e, "Handler exception for " + event.getClass().getSimpleName());
      } finally {
        metrics.recordHandlerTime(System.nanoTime() - start);
      }
    }
    metrics.incrementEventsProcessed();
  }

  @Override
  public int handlerCount(Class<? extends ZEvent> eventType) {
    List<HandlerEntry<?>> entries = handlers.get(eventType);
    if (entries == null) {
      return 0;
    }
    return (int) entries.stream().filter(e -> !e.isCancelled()).count();
  }

  @Override
  public void clear() {
    handlers.clear();
  }

  private void dispatchError(Throwable cause, String message) {
    List<HandlerEntry<?>> errorHandlers = handlers.get(ErrorEvent.class);
    if (errorHandlers == null || errorHandlers.isEmpty()) {
      return;
    }
    ErrorEvent errorEvent = errorEventPool.borrow();
    errorEvent.reset(cause, message);
    dispatch(errorEvent);
    errorEventPool.release(errorEvent);
  }

  /**
   * Internal entry pairing a handler with its cancellation state.
   */
  private static final class HandlerEntry<T extends ZEvent> implements HandlerRegistration {

    private final EventHandler<T> handler;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    HandlerEntry(EventHandler<T> handler) {
      this.handler = handler;
    }

    EventHandler<T> handler() {
      return handler;
    }

    @Override
    public void cancel() {
      cancelled.set(true);
    }

    @Override
    public boolean isCancelled() {
      return cancelled.get();
    }
  }
}
