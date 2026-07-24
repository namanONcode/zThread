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

import io.github.namanoncode.zthread.exception.ConfigurationException;
import java.util.Locale;

/**
 * Platform detection utilities.
 *
 * <p>Verifies that the current operating system and architecture are supported
 * by zThread.
 */
public final class Platform {

  /** The detected operating system name. */
  public static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

  /** The detected architecture. */
  public static final String OS_ARCH = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

  private Platform() {
    throw new AssertionError("Utility class — do not instantiate");
  }

  /**
   * Returns whether the current OS is Linux.
   *
   * @return true if running on Linux
   */
  public static boolean isLinux() {
    return OS_NAME.contains("linux");
  }

  /**
   * Returns whether the current architecture is x86_64/amd64.
   *
   * @return true if running on x86_64
   */
  public static boolean isX8664() {
    return OS_ARCH.contains("amd64") || OS_ARCH.contains("x86_64");
  }

  /**
   * Returns whether the current architecture is aarch64/arm64.
   *
   * @return true if running on aarch64
   */
  public static boolean isAarch64() {
    return OS_ARCH.contains("aarch64") || OS_ARCH.contains("arm64");
  }

  /**
   * Verifies that the current platform is supported and throws if not.
   *
   * @throws ConfigurationException if the platform is not Linux
   */
  public static void ensureLinux() {
    if (!isLinux()) {
      throw new ConfigurationException(
          "zThread requires Linux. Current OS: " + System.getProperty("os.name"));
    }
  }
}
