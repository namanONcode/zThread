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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PreconditionsTest {

  @Test
  void requireNonNullPasses() {
    assertThat(Preconditions.requireNonNull("hello", "value")).isEqualTo("hello");
  }

  @Test
  void requireNonNullRejectsNull() {
    assertThatThrownBy(() -> Preconditions.requireNonNull(null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("value");
  }

  @Test
  void requireNonBlankPasses() {
    assertThat(Preconditions.requireNonBlank("hello", "name")).isEqualTo("hello");
  }

  @Test
  void requireNonBlankRejectsBlank() {
    assertThatThrownBy(() -> Preconditions.requireNonBlank("  ", "name"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void requirePositiveIntPasses() {
    assertThat(Preconditions.requirePositive(5, "value")).isEqualTo(5);
  }

  @Test
  void requirePositiveIntRejectsZero() {
    assertThatThrownBy(() -> Preconditions.requirePositive(0, "value"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void requirePositiveLongRejectsNegative() {
    assertThatThrownBy(() -> Preconditions.requirePositive(-1L, "value"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void requireValidFdPasses() {
    assertThat(Preconditions.requireValidFd(0, "fd")).isZero();
    assertThat(Preconditions.requireValidFd(42, "fd")).isEqualTo(42);
  }

  @Test
  void requireValidFdRejectsNegative() {
    assertThatThrownBy(() -> Preconditions.requireValidFd(-1, "fd"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
