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
package io.github.namanoncode.zthread.util;

/**
 * Argument validation utilities.
 *
 * <p>Provides static methods for common precondition checks. Methods throw
 * {@link IllegalArgumentException} on failure with descriptive messages.
 */
public final class Preconditions {

  private Preconditions() {
    throw new AssertionError("Utility class — do not instantiate");
  }

  /**
   * Ensures the argument is not null.
   *
   * @param <T> the type
   * @param value the value to check
   * @param name the parameter name for error messages
   * @return the value if not null
   * @throws IllegalArgumentException if value is null
   */
  public static <T> T requireNonNull(T value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }

  /**
   * Ensures the string is not null or blank.
   *
   * @param value the string to check
   * @param name the parameter name for error messages
   * @return the value if not blank
   * @throws IllegalArgumentException if value is null or blank
   */
  public static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be null or blank");
    }
    return value;
  }

  /**
   * Ensures the value is positive (greater than zero).
   *
   * @param value the value to check
   * @param name the parameter name for error messages
   * @return the value if positive
   * @throws IllegalArgumentException if value is not positive
   */
  public static int requirePositive(int value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive: " + value);
    }
    return value;
  }

  /**
   * Ensures the value is positive (greater than zero).
   *
   * @param value the value to check
   * @param name the parameter name for error messages
   * @return the value if positive
   * @throws IllegalArgumentException if value is not positive
   */
  public static long requirePositive(long value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive: " + value);
    }
    return value;
  }

  /**
   * Ensures the value is non-negative (zero or greater).
   *
   * @param value the value to check
   * @param name the parameter name for error messages
   * @return the value if non-negative
   * @throws IllegalArgumentException if value is negative
   */
  public static long requireNonNegative(long value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException(name + " must not be negative: " + value);
    }
    return value;
  }

  /**
   * Ensures the value is a valid file descriptor (non-negative).
   *
   * @param fd the file descriptor to check
   * @param name the parameter name for error messages
   * @return the fd if valid
   * @throws IllegalArgumentException if fd is negative
   */
  public static int requireValidFd(int fd, String name) {
    if (fd < 0) {
      throw new IllegalArgumentException(name + " is not a valid file descriptor: " + fd);
    }
    return fd;
  }
}
