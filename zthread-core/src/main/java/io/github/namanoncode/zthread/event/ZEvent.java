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
 * Base interface for all events in the zThread runtime.
 *
 * <p>This is a sealed interface — all event types are defined within the zThread
 * library. Events carry data about kernel notifications (socket I/O, timers,
 * signals, file changes) or user-defined custom events.
 *
 * <p>Events are dispatched to registered handlers on the event loop thread.
 *
 * @see SocketEvent
 * @see TimerEvent
 * @see SignalEvent
 * @see FileEvent
 * @see CustomEvent
 * @see ErrorEvent
 * @see ShutdownEvent
 */
public sealed interface ZEvent
    permits SocketEvent, TimerEvent, SignalEvent, FileEvent, CustomEvent, ErrorEvent,
        ShutdownEvent {

  /**
   * Returns the timestamp (in nanoseconds, from {@link System#nanoTime()}) when this
   * event was created.
   *
   * @return the creation timestamp in nanoseconds
   */
  long timestampNanos();
}
