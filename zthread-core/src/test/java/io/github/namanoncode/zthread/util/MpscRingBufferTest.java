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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MpscRingBufferTest {

  @Test
  void capacityIsRoundedUpToPowerOfTwo() {
    MpscRingBuffer buffer = new MpscRingBuffer(100);
    assertThat(buffer.capacity()).isEqualTo(128);
  }

  @Test
  void capacityPowerOfTwoUnchanged() {
    MpscRingBuffer buffer = new MpscRingBuffer(64);
    assertThat(buffer.capacity()).isEqualTo(64);
  }

  @Test
  void offerAndPollSingleItem() {
    MpscRingBuffer buffer = new MpscRingBuffer(64);
    assertThat(buffer.offer("42")).isTrue();
    assertThat(buffer.poll()).isEqualTo("42");
    assertThat(buffer.poll()).isNull();
  }

  @Test
  void offerAndPollMultipleItems() {
    MpscRingBuffer buffer = new MpscRingBuffer(64);
    for (int i = 1; i <= 10; i++) {
      assertThat(buffer.offer("item" + i)).isTrue();
    }
    for (int i = 1; i <= 10; i++) {
      assertThat(buffer.poll()).isEqualTo("item" + i);
    }
    assertThat(buffer.isEmpty()).isTrue();
  }

  @Test
  void offerReturnsFalseWhenFull() {
    MpscRingBuffer buffer = new MpscRingBuffer(4);
    int capacity = buffer.capacity();
    for (int i = 0; i < capacity; i++) {
      assertThat(buffer.offer("item" + i)).isTrue();
    }
    assertThat(buffer.offer("full")).isFalse();
  }

  @Test
  void wrapAround() {
    MpscRingBuffer buffer = new MpscRingBuffer(4);
    int capacity = buffer.capacity();

    for (int round = 0; round < 3; round++) {
      for (int i = 0; i < capacity; i++) {
        assertThat(buffer.offer("item" + (round * capacity + i))).isTrue();
      }
      for (int i = 0; i < capacity; i++) {
        assertThat(buffer.poll()).isEqualTo("item" + (round * capacity + i));
      }
    }
  }

  @Test
  void sizeReflectsState() {
    MpscRingBuffer buffer = new MpscRingBuffer(64);
    assertThat(buffer.size()).isZero();
    assertThat(buffer.isEmpty()).isTrue();

    buffer.offer("test");
    assertThat(buffer.size()).isEqualTo(1);
    assertThat(buffer.isEmpty()).isFalse();

    buffer.poll();
    assertThat(buffer.size()).isZero();
  }

  @Test
  void rejectsNullValue() {
    MpscRingBuffer buffer = new MpscRingBuffer(64);
    assertThatThrownBy(() -> buffer.offer(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidCapacity() {
    assertThatThrownBy(() -> new MpscRingBuffer(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MpscRingBuffer(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void concurrentProducersSingleConsumer() throws Exception {
    int numProducers = 4;
    int itemsPerProducer = 10_000;
    MpscRingBuffer buffer = new MpscRingBuffer(4096);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numProducers);

    ExecutorService executor = Executors.newFixedThreadPool(numProducers);
    for (int p = 0; p < numProducers; p++) {
      final int producerId = p;
      executor.submit(() -> {
        try {
          startLatch.await();
          for (int i = 0; i < itemsPerProducer; i++) {
            Long value = producerId * 100_000L + i + 1;
            while (!buffer.offer(value)) {
              Thread.onSpinWait();
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();

    List<Long> consumed = new ArrayList<>();
    int totalExpected = numProducers * itemsPerProducer;

    while (consumed.size() < totalExpected) {
      Object value = buffer.poll();
      if (value != null) {
        consumed.add((Long) value);
      } else {
        Thread.onSpinWait();
      }
    }

    doneLatch.await();
    executor.shutdown();

    assertThat(consumed).hasSize(totalExpected);
    assertThat(consumed).doesNotHaveDuplicates();
  }
}
