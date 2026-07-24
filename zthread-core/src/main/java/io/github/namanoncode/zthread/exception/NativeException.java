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
package io.github.namanoncode.zthread.exception;

/**
 * Exception thrown when a native system call fails.
 *
 * <p>Wraps the errno value and provides a human-readable message.
 */
public class NativeException extends ZThreadException {

  private final int errno;

  /**
   * Creates a new native exception.
   *
   * @param message the error message
   * @param errno the Linux errno value
   */
  public NativeException(String message, int errno) {
    super(message + " (errno=" + errno + ")");
    this.errno = errno;
  }

  /**
   * Creates a new native exception with a cause.
   *
   * @param message the error message
   * @param errno the Linux errno value
   * @param cause the underlying cause
   */
  public NativeException(String message, int errno, Throwable cause) {
    super(message + " (errno=" + errno + ")", cause);
    this.errno = errno;
  }

  /**
   * Returns the Linux errno value.
   *
   * @return the errno
   */
  public int errno() {
    return errno;
  }
}
