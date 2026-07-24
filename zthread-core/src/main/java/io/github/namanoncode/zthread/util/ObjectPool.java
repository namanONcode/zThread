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

import java.util.function.Supplier;

/**
 * Generic fixed-size object pool for reusable objects.
 *
 * <p>Reduces GC pressure by recycling event objects instead of allocating new ones.
 * The pool is backed by a simple ring buffer. If the pool is exhausted, new objects
 * are allocated via the supplier.
 *
 * <p>Thread safety: This pool is designed for single-threaded access from the
 * event loop thread. Use from multiple threads requires external synchronization.
 *
 * @param <T> the type of pooled objects
 */
public final class ObjectPool<T> {

  private final Object[] pool;
  private final Supplier<T> factory;
  private final int capacity;
  private int head;
  private int size;

  /**
   * Creates a new object pool.
   *
   * @param capacity the pool capacity
   * @param factory the factory for creating new instances when the pool is empty
   */
  public ObjectPool(int capacity, Supplier<T> factory) {
    Preconditions.requirePositive(capacity, "capacity");
    Preconditions.requireNonNull(factory, "factory");
    this.capacity = capacity;
    this.factory = factory;
    this.pool = new Object[capacity];
    this.head = 0;
    this.size = 0;

    for (int i = 0; i < capacity; i++) {
      pool[i] = factory.get();
      size++;
    }
  }

  /**
   * Borrows an object from the pool.
   *
   * <p>If the pool is empty, a new object is created via the factory.
   *
   * @return a pooled or new object
   */
  @SuppressWarnings("unchecked")
  public T borrow() {
    if (size == 0) {
      return factory.get();
    }
    int index = head;
    T obj = (T) pool[index];
    pool[index] = null;
    head = (head + 1) % capacity;
    size--;
    return obj;
  }

  /**
   * Returns an object to the pool.
   *
   * <p>If the pool is full, the object is silently discarded (left for GC).
   *
   * @param obj the object to return
   */
  public void release(T obj) {
    if (size >= capacity) {
      return;
    }
    int index = (head + size) % capacity;
    pool[index] = obj;
    size++;
  }

  /**
   * Returns the current number of objects in the pool.
   *
   * @return the pool size
   */
  public int size() {
    return size;
  }

  /**
   * Returns the pool capacity.
   *
   * @return the capacity
   */
  public int capacity() {
    return capacity;
  }
}
