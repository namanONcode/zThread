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
 * Event fired when an error occurs during event processing.
 *
 * @see ZEvent
 */
public final class ErrorEvent implements ZEvent {

  private Throwable cause;
  private String message;
  private long timestampNanos;


  /**
   * Resets this event with new values.
   *
   * @param cause the exception that caused the error
   * @param message a descriptive error message
   * @return this event for chaining
   */
  public ErrorEvent reset(Throwable cause, String message) {
    this.cause = cause;
    this.message = message;
    this.timestampNanos = System.nanoTime();
    return this;
  }

  /**
   * Returns the exception that caused the error.
   *
   * @return the cause, may be null
   */
  public Throwable cause() {
    return cause;
  }

  /**
   * Returns the error message.
   *
   * @return the message
   */
  public String message() {
    return message;
  }

  @Override
  public long timestampNanos() {
    return timestampNanos;
  }

  @Override
  public String toString() {
    return "ErrorEvent{message=" + message + ", cause=" + cause + "}";
  }
}
