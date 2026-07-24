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
 * Interface for the native polling mechanism.
 *
 * <p>Abstracts the OS-level I/O multiplexer. On Linux, this wraps epoll operations.
 * The poller blocks the event loop thread until kernel events arrive or a timeout occurs.
 *
 * <p>Thread safety: {@link #poll(int)} must only be called from the event loop thread.
 * {@link #addFd(int, int, long)} and {@link #removeFd(int)} are thread-safe.
 */
public interface NativePoller extends AutoCloseable {

  /**
   * Blocks until events are available or the timeout expires.
   *
   * @param timeoutMillis the maximum wait time in milliseconds, or -1 for indefinite
   * @return the number of events ready
   */
  int poll(int timeoutMillis);

  /**
   * Retrieves the events mask for the event at the given index after a poll.
   *
   * @param index the event index (0-based, less than the value returned by poll)
   * @return the events mask (e.g., EPOLLIN, EPOLLOUT)
   */
  int getEvents(int index);

  /**
   * Retrieves the user data for the event at the given index after a poll.
   *
   * @param index the event index
   * @return the user data associated with this file descriptor
   */
  long getUserData(int index);

  /**
   * Registers a file descriptor for monitoring.
   *
   * @param fd the file descriptor to monitor
   * @param events the events to monitor (e.g., EPOLLIN)
   * @param userData user data to associate with this fd
   */
  void addFd(int fd, int events, long userData);

  /**
   * Modifies the events being monitored for a file descriptor.
   *
   * @param fd the file descriptor
   * @param events the new events mask
   * @param userData the new user data
   */
  void modifyFd(int fd, int events, long userData);

  /**
   * Removes a file descriptor from monitoring.
   *
   * @param fd the file descriptor to remove
   */
  void removeFd(int fd);

  /**
   * Closes the poller and releases native resources.
   */
  @Override
  void close();
}
