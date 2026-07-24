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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.namanoncode.zthread.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

class ZRuntimeBuilderTest {

  @Test
  void defaultBuildThrowsWhenNoImplementation() {
    assertThatThrownBy(() -> ZRuntime.create())
        .isInstanceOf(ConfigurationException.class)
        .hasMessageContaining("No ZRuntime implementation found");
  }

  @Test
  void builderSetsDefaults() {
    ZRuntimeBuilder builder = ZRuntime.builder();
    assertThat(builder.threadName()).isEqualTo("zthread-event-loop");
    assertThat(builder.bufferSize()).isEqualTo(4096);
    assertThat(builder.maxEventsPerPoll()).isEqualTo(64);
    assertThat(builder.metricsEnabled()).isTrue();
    assertThat(builder.debugEnabled()).isFalse();
  }

  @Test
  void builderAcceptsCustomValues() {
    ZRuntimeBuilder builder = ZRuntime.builder()
        .threadName("custom-loop")
        .bufferSize(8192)
        .maxEventsPerPoll(128)
        .metricsEnabled(false)
        .debugEnabled(true);

    assertThat(builder.threadName()).isEqualTo("custom-loop");
    assertThat(builder.bufferSize()).isEqualTo(8192);
    assertThat(builder.maxEventsPerPoll()).isEqualTo(128);
    assertThat(builder.metricsEnabled()).isFalse();
    assertThat(builder.debugEnabled()).isTrue();
  }

  @Test
  void builderRejectsNullThreadName() {
    assertThatThrownBy(() -> ZRuntime.builder().threadName(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void builderRejectsBlankThreadName() {
    assertThatThrownBy(() -> ZRuntime.builder().threadName("  "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void builderRejectsZeroBufferSize() {
    assertThatThrownBy(() -> ZRuntime.builder().bufferSize(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void builderRejectsNegativeMaxEvents() {
    assertThatThrownBy(() -> ZRuntime.builder().maxEventsPerPoll(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void builderEnforcesMinimumBufferSize() {
    ZRuntimeBuilder builder = ZRuntime.builder().bufferSize(10);
    assertThat(builder.bufferSize()).isGreaterThanOrEqualTo(64);
  }
}
