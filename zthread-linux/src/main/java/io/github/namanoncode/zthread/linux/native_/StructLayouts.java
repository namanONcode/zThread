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
package io.github.namanoncode.zthread.linux.native_;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Memory layouts for Linux kernel structures used with the FFM API.
 *
 * <p>All layouts are verified against the x86_64 Linux kernel headers.
 *
 * <h2>Critical: epoll_event packing</h2>
 * <p>On x86_64, {@code struct epoll_event} uses {@code __attribute__((__packed__))},
 * making it <b>12 bytes</b>, not 16. The events field (uint32) is immediately followed
 * by the data union (uint64) with no padding.
 */
public final class StructLayouts {

  private StructLayouts() {
    throw new AssertionError("Utility class");
  }

  /**
   * {@code struct epoll_event} — 12 bytes, packed on x86_64.
   * <pre>
   * uint32_t events;  // offset 0, size 4
   * uint64_t data;    // offset 4, size 8 (using fd as u64)
   * </pre>
   */
  public static final MemoryLayout EPOLL_EVENT = MemoryLayout.structLayout(
      ValueLayout.JAVA_INT.withName("events"),
      ValueLayout.JAVA_LONG_UNALIGNED.withName("data")
  ).withName("epoll_event");

  /** Size of a single epoll_event struct in bytes. */
  public static final long EPOLL_EVENT_SIZE = EPOLL_EVENT.byteSize();

  /**
   * {@code struct timespec} — 16 bytes.
   * <pre>
   * long tv_sec;   // seconds
   * long tv_nsec;  // nanoseconds
   * </pre>
   */
  public static final MemoryLayout TIMESPEC = MemoryLayout.structLayout(
      ValueLayout.JAVA_LONG.withName("tv_sec"),
      ValueLayout.JAVA_LONG.withName("tv_nsec")
  ).withName("timespec");

  /**
   * {@code struct itimerspec} — 32 bytes.
   * <pre>
   * struct timespec it_interval;  // timer period
   * struct timespec it_value;     // initial expiration
   * </pre>
   */
  public static final MemoryLayout ITIMERSPEC = MemoryLayout.structLayout(
      TIMESPEC.withName("it_interval"),
      TIMESPEC.withName("it_value")
  ).withName("itimerspec");

  /** Size of itimerspec in bytes. */
  public static final long ITIMERSPEC_SIZE = ITIMERSPEC.byteSize();

  /**
   * {@code struct sockaddr_in} — 16 bytes.
   */
  public static final MemoryLayout SOCKADDR_IN = MemoryLayout.structLayout(
      ValueLayout.JAVA_SHORT.withName("sin_family"),
      ValueLayout.JAVA_SHORT.withName("sin_port"),
      ValueLayout.JAVA_INT.withName("sin_addr"),
      MemoryLayout.sequenceLayout(8, ValueLayout.JAVA_BYTE).withName("sin_zero")
  ).withName("sockaddr_in");

  /** Size of sockaddr_in. */
  public static final long SOCKADDR_IN_SIZE = SOCKADDR_IN.byteSize();

  // ---- Helper methods for reading/writing struct fields ----

  /**
   * Writes an epoll_event struct at the given segment.
   *
   * @param segment the memory segment (must be at least 12 bytes)
   * @param offset the byte offset into the segment
   * @param events the epoll events mask
   * @param data the user data (typically an fd cast to long)
   */
  public static void writeEpollEvent(MemorySegment segment, long offset,
      int events, long data) {
    segment.set(ValueLayout.JAVA_INT, offset, events);
    segment.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, data);
  }

  /**
   * Reads the events field from an epoll_event struct.
   *
   * @param segment the memory segment
   * @param offset the byte offset
   * @return the events mask
   */
  public static int readEpollEvents(MemorySegment segment, long offset) {
    return segment.get(ValueLayout.JAVA_INT, offset);
  }

  /**
   * Reads the data field from an epoll_event struct.
   *
   * @param segment the memory segment
   * @param offset the byte offset
   * @return the user data
   */
  public static long readEpollData(MemorySegment segment, long offset) {
    return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4);
  }

  /**
   * Writes an itimerspec struct for a one-shot or periodic timer.
   *
   * @param segment the memory segment (must be at least 32 bytes)
   * @param intervalSec the interval seconds (0 for one-shot)
   * @param intervalNsec the interval nanoseconds
   * @param valueSec the initial value seconds
   * @param valueNsec the initial value nanoseconds
   */
  public static void writeItimerspec(MemorySegment segment,
      long intervalSec, long intervalNsec,
      long valueSec, long valueNsec) {
    segment.set(ValueLayout.JAVA_LONG, 0, intervalSec);
    segment.set(ValueLayout.JAVA_LONG, 8, intervalNsec);
    segment.set(ValueLayout.JAVA_LONG, 16, valueSec);
    segment.set(ValueLayout.JAVA_LONG, 24, valueNsec);
  }
}
