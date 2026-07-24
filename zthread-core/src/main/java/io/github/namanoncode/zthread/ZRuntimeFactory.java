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

/**
 * Service provider interface for creating {@link ZRuntime} instances.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}. The
 * {@code zthread-linux} module provides the Linux-specific implementation.
 *
 * <p>This interface follows the plug-and-play pattern: simply adding the Linux module
 * to the classpath enables it automatically without any code changes.
 *
 * @see ZRuntimeBuilder
 */
@FunctionalInterface
public interface ZRuntimeFactory {

  /**
   * Creates a new {@link ZRuntime} instance with the given configuration.
   *
   * @param config the runtime configuration
   * @return a new runtime instance, not yet started
   */
  ZRuntime create(ZRuntimeConfig config);
}
