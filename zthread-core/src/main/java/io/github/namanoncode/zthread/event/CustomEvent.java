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
package io.github.namanoncode.zthread.event;

/**
 * User-defined custom event that can be posted from any thread.
 *
 * <p>Custom events are the primary mechanism for cross-thread communication.
 * When posted via {@link io.github.namanoncode.zthread.ZRuntime#post(CustomEvent)},
 * the event is enqueued in a lock-free ring buffer and the event loop is woken
 * via an eventfd write.
 *
 * @see ZEvent
 */
public final class CustomEvent implements ZEvent {

  private Object payload;
  private long timestampNanos;

  /**
   * Creates a custom event with the given payload.
   *
   * @param payload the event payload, may be null
   */
  public CustomEvent(Object payload) {
    this.payload = payload;
    this.timestampNanos = System.nanoTime();
  }

  /** Creates an uninitialized custom event. Used by the object pool. */
  public CustomEvent() {
    this.timestampNanos = System.nanoTime();
  }

  /**
   * Resets this event with a new payload. Used for object pool recycling.
   *
   * @param payload the new payload
   * @return this event for chaining
   */
  public CustomEvent reset(Object payload) {
    this.payload = payload;
    this.timestampNanos = System.nanoTime();
    return this;
  }

  /**
   * Returns the event payload.
   *
   * @return the payload, may be null
   */
  public Object payload() {
    return payload;
  }

  /**
   * Returns the event payload cast to the specified type.
   *
   * @param <T> the expected type
   * @param type the class to cast to
   * @return the payload cast to T
   * @throws ClassCastException if the payload is not of the expected type
   */
  @SuppressWarnings("unchecked")
  public <T> T payload(Class<T> type) {
    return type.cast(payload);
  }

  @Override
  public long timestampNanos() {
    return timestampNanos;
  }

  @Override
  public String toString() {
    return "CustomEvent{payload=" + payload + "}";
  }
}
