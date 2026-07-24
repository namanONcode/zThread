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
 * Event fired when a Linux signal is received via signalfd.
 *
 * <p>The signal must have been blocked with {@code sigprocmask} and registered
 * with the runtime for delivery via signalfd.
 *
 * @see ZEvent
 */
public final class SignalEvent implements ZEvent {

  private int signalNumber;
  private int senderPid;
  private int senderUid;
  private long timestampNanos;

  /** Creates an uninitialized signal event. */
  public SignalEvent() {}

  /**
   * Resets this event with new values.
   *
   * @param signo the signal number
   * @param pid the sender's process ID
   * @param uid the sender's user ID
   * @return this event for chaining
   */
  public SignalEvent reset(int signo, int pid, int uid) {
    this.signalNumber = signo;
    this.senderPid = pid;
    this.senderUid = uid;
    this.timestampNanos = System.nanoTime();
    return this;
  }

  /**
   * Returns the signal number (e.g., SIGINT=2, SIGTERM=15).
   *
   * @return the signal number
   */
  public int signalNumber() {
    return signalNumber;
  }

  /**
   * Returns the PID of the process that sent the signal.
   *
   * @return the sender PID
   */
  public int senderPid() {
    return senderPid;
  }

  /**
   * Returns the UID of the user that sent the signal.
   *
   * @return the sender UID
   */
  public int senderUid() {
    return senderUid;
  }

  @Override
  public long timestampNanos() {
    return timestampNanos;
  }

  @Override
  public String toString() {
    return "SignalEvent{signal=" + signalNumber + ", pid=" + senderPid + "}";
  }
}
