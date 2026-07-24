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

import org.junit.jupiter.api.Test;

class ObjectPoolTest {

  @Test
  void borrowReturnsPreallocatedObjects() {
    ObjectPool<StringBuilder> pool = new ObjectPool<>(4, StringBuilder::new);
    assertThat(pool.size()).isEqualTo(4);

    StringBuilder sb = pool.borrow();
    assertThat(sb).isNotNull();
    assertThat(pool.size()).isEqualTo(3);
  }

  @Test
  void releaseReturnsObjectToPool() {
    ObjectPool<StringBuilder> pool = new ObjectPool<>(4, StringBuilder::new);
    StringBuilder sb = pool.borrow();
    pool.release(sb);
    assertThat(pool.size()).isEqualTo(4);
  }

  @Test
  void borrowFromEmptyPoolCreatesNew() {
    ObjectPool<StringBuilder> pool = new ObjectPool<>(1, StringBuilder::new);
    pool.borrow();
    assertThat(pool.size()).isZero();

    StringBuilder sb = pool.borrow();
    assertThat(sb).isNotNull();
  }

  @Test
  void releaseToFullPoolDiscardsSilently() {
    ObjectPool<StringBuilder> pool = new ObjectPool<>(2, StringBuilder::new);
    pool.release(new StringBuilder());
    assertThat(pool.size()).isEqualTo(2);
  }

  @Test
  void capacityIsCorrect() {
    ObjectPool<Object> pool = new ObjectPool<>(16, Object::new);
    assertThat(pool.capacity()).isEqualTo(16);
  }
}
