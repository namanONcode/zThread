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

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Lock-free multi-producer single-consumer (MPSC) ring buffer.
 *
 * <p>Designed for the producer→event-loop communication pattern: multiple threads
 * can {@link #offer(long)} values concurrently, while a single event loop thread
 * calls {@link #poll()}. The buffer capacity is always a power of two.
 *
 * <h2>False Sharing Prevention</h2>
 * <p>Producer and consumer indices are padded to occupy separate cache lines
 * (64 bytes apart) to prevent false sharing.
 *
 * <h2>Memory Ordering</h2>
 * <p>Uses {@link AtomicLongArray} for the buffer slots, providing volatile read/write
 * semantics on each element. The producer sequence uses CAS for contention resolution.
 *
 * <p>Thread safety: {@link #offer(long)} is safe for multiple concurrent producers.
 * {@link #poll()} must only be called from a single consumer thread.
 *
 * @see io.github.namanoncode.zthread.EventLoop
 */
@SuppressWarnings("PMD")
public final class MpscRingBuffer {

  private final java.util.concurrent.atomic.AtomicReferenceArray<Object> buffer;
  private final int mask;
  private final int capacity;

  // Padded producer sequence — each in its own cache line
  @SuppressWarnings("unused")
  private long p01;
  @SuppressWarnings("unused")
  private long p02;
  @SuppressWarnings("unused")
  private long p03;
  @SuppressWarnings("unused")
  private long p04;
  @SuppressWarnings("unused")
  private long p05;
  @SuppressWarnings("unused")
  private long p06;
  @SuppressWarnings("unused")
  private long p07;

  private volatile long producerSequence;

  @SuppressWarnings("unused")
  private long p11;
  @SuppressWarnings("unused")
  private long p12;
  @SuppressWarnings("unused")
  private long p13;
  @SuppressWarnings("unused")
  private long p14;
  @SuppressWarnings("unused")
  private long p15;
  @SuppressWarnings("unused")
  private long p16;
  @SuppressWarnings("unused")
  private long p17;

  // Padded consumer sequence
  @SuppressWarnings("unused")
  private long p21;
  @SuppressWarnings("unused")
  private long p22;
  @SuppressWarnings("unused")
  private long p23;
  @SuppressWarnings("unused")
  private long p24;
  @SuppressWarnings("unused")
  private long p25;
  @SuppressWarnings("unused")
  private long p26;
  @SuppressWarnings("unused")
  private long p27;

  private long consumerSequence;

  @SuppressWarnings("unused")
  private long p31;
  @SuppressWarnings("unused")
  private long p32;
  @SuppressWarnings("unused")
  private long p33;
  @SuppressWarnings("unused")
  private long p34;
  @SuppressWarnings("unused")
  private long p35;
  @SuppressWarnings("unused")
  private long p36;
  @SuppressWarnings("unused")
  private long p37;

  /**
   * Creates a new ring buffer with the given capacity, rounded up to a power of two.
   *
   * @param requestedCapacity the minimum capacity, must be positive
   * @throws IllegalArgumentException if capacity is not positive
   */
  public MpscRingBuffer(int requestedCapacity) {
    Preconditions.requirePositive(requestedCapacity, "capacity");
    this.capacity = nextPowerOfTwo(requestedCapacity);
    this.mask = capacity - 1;
    this.buffer = new java.util.concurrent.atomic.AtomicReferenceArray<>(capacity);
  }

  /**
   * Offers a value to the ring buffer from any producer thread.
   *
   * <p>This method uses CAS on the producer sequence to resolve contention
   * between multiple producers.
   *
   * @param value the value to enqueue (must not be null)
   * @return {@code true} if the value was successfully enqueued, {@code false} if full
   */
  public boolean offer(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Cannot enqueue null");
    }

    long currentProducer;
    long nextProducer;
    do {
      currentProducer = producerSequence;
      nextProducer = currentProducer + 1;
      if (nextProducer - consumerSequence > capacity) {
        return false;
      }
    } while (!compareAndSetProducerSequence(currentProducer, nextProducer));

    int index = (int) (currentProducer & mask);
    buffer.set(index, value);
    return true;
  }

  /**
   * Polls a value from the ring buffer. Must only be called from the consumer thread.
   *
   * @return the next value, or null if empty
   */
  public Object poll() {
    long currentConsumer = consumerSequence;
    if (producerSequence == currentConsumer) {
      return null;
    }

    int index = (int) (currentConsumer & mask);
    Object value = buffer.get(index);
    if (value == null) {
      int spins = 0;
      do {
        Thread.onSpinWait();
        spins++;
        if (spins > 10_000) {
          Thread.yield();
        }
        value = buffer.get(index);
      } while (value == null);
    }
    buffer.set(index, null);
    consumerSequence++;
    return value;
  }

  /**
   * Returns the number of elements currently in the buffer.
   *
   * <p>This is an approximate value due to concurrent modifications.
   *
   * @return the approximate size
   */
  public int size() {
    return (int) (producerSequence - consumerSequence);
  }

  /**
   * Returns whether the buffer is empty.
   *
   * @return true if empty
   */
  public boolean isEmpty() {
    return producerSequence == consumerSequence;
  }

  /**
   * Returns the buffer capacity.
   *
   * @return the capacity (always a power of two)
   */
  public int capacity() {
    return capacity;
  }

  private static final java.lang.invoke.VarHandle PRODUCER_SEQUENCE;

  static {
    try {
      PRODUCER_SEQUENCE =
          java.lang.invoke.MethodHandles.lookup()
              .findVarHandle(MpscRingBuffer.class, "producerSequence", long.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private boolean compareAndSetProducerSequence(long expected, long newValue) {
    return PRODUCER_SEQUENCE.compareAndSet(this, expected, newValue);
  }

  private static int nextPowerOfTwo(int value) {
    if (value <= 0) {
      return 1;
    }
    return Integer.highestOneBit(value - 1) << 1;
  }
}
