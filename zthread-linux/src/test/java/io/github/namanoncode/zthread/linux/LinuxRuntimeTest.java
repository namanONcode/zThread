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
package io.github.namanoncode.zthread.linux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.namanoncode.zthread.ZRuntime;
import io.github.namanoncode.zthread.event.CustomEvent;
import io.github.namanoncode.zthread.event.FileEvent;
import io.github.namanoncode.zthread.linux.native_.LinuxConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs(OS.LINUX)
class LinuxRuntimeTest {

  private ZRuntime runtime;

  @BeforeEach
  void setUp() {
    runtime = ZRuntime.builder()
        .threadName("test-loop")
        .bufferSize(1024)
        .build();
    runtime.start();
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    if (runtime != null) {
      runtime.shutdown();
      runtime.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void customEventsAreDispatched() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(5);
    CopyOnWriteArrayList<String> received = new CopyOnWriteArrayList<>();

    runtime.on(CustomEvent.class, event -> {
      received.add(event.payload(String.class));
      latch.countDown();
    });

    for (int i = 0; i < 5; i++) {
      runtime.post(new CustomEvent("msg" + i));
    }

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(received).containsExactly("msg0", "msg1", "msg2", "msg3", "msg4");
    
    // Check thread was indeed the event loop
    assertThat(runtime.metrics().snapshot().totalEventsProcessed()).isGreaterThanOrEqualTo(5);
  }

  @Test
  void timersAreFired() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(3);
    AtomicInteger count = new AtomicInteger();

    runtime.schedulePeriodic(() -> {
      count.incrementAndGet();
      latch.countDown();
    }, 10, 10, TimeUnit.MILLISECONDS);

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(count.get()).isGreaterThanOrEqualTo(3);
  }

  @Test
  void delayedTimersAreFiredOnce() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger count = new AtomicInteger();

    runtime.schedule(() -> {
      count.incrementAndGet();
      latch.countDown();
    }, 50, TimeUnit.MILLISECONDS);

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    
    // Wait a bit more to ensure it doesn't fire again
    Thread.sleep(100);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  void inotifyWatchDetectsFileChanges(@TempDir Path tempDir) throws Exception {
    CopyOnWriteArrayList<FileEvent> events = new CopyOnWriteArrayList<>();
    runtime.on(FileEvent.class, events::add);

    runtime.watch(tempDir, LinuxConstants.IN_CREATE | LinuxConstants.IN_MODIFY);

    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, "hello");

    await().atMost(Duration.ofSeconds(2)).until(() -> !events.isEmpty());

    assertThat(events).anyMatch(e -> e.isCreate() && "test.txt".equals(e.name()));
  }

  @Test
  void loadTestCustomEvents() throws InterruptedException {
    int numEvents = 100_000;
    CountDownLatch latch = new CountDownLatch(numEvents);

    runtime.on(CustomEvent.class, event -> latch.countDown());

    // Spin up producers
    Thread[] producers = new Thread[4];
    for (int i = 0; i < 4; i++) {
      producers[i] = new Thread(() -> {
        for (int j = 0; j < numEvents / 4; j++) {
          CustomEvent event = new CustomEvent(j);
          while (true) {
            try {
              runtime.post(event);
              break;
            } catch (io.github.namanoncode.zthread.exception.EventLoopException e) {
              Thread.onSpinWait();
            }
          }
        }
      });
      producers[i].start();
    }

    for (Thread t : producers) {
      t.join();
    }

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(runtime.metrics().snapshot().totalEventsProcessed())
        .isGreaterThanOrEqualTo(numEvents);
  }
}
