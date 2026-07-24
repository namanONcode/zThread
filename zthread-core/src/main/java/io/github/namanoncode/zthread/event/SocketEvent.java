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
package io.github.namanoncode.zthread.event;

/**
 * Event representing socket I/O readiness.
 *
 * <p>Fired when a socket has data ready to read, is ready to write, encountered
 * an error, or the remote side hung up.
 *
 * <p>This is a mutable, poolable event object. Fields are set by the event loop
 * before dispatch and should not be modified by handlers.
 *
 * @see ZEvent
 */
public final class SocketEvent implements ZEvent {

  /** Socket is readable. */
  public static final int READABLE = 0x001;

  /** Socket is writable. */
  public static final int WRITABLE = 0x004;

  /** Socket encountered an error. */
  public static final int ERROR = 0x008;

  /** Remote side closed the connection. */
  public static final int HANGUP = 0x010;

  /** Remote side closed its write end (half-close). */
  public static final int READ_HANGUP = 0x2000;

  private int fileDescriptor;
  private int eventMask;
  private long timestampNanos;

  /** Creates an uninitialized socket event. Used by the object pool. */
  public SocketEvent() {}

  /**
   * Resets this event with new values. Called by the event loop before dispatch.
   *
   * @param fd the socket file descriptor
   * @param mask the epoll event mask
   * @return this event for chaining
   */
  public SocketEvent reset(int fd, int mask) {
    this.fileDescriptor = fd;
    this.eventMask = mask;
    this.timestampNanos = System.nanoTime();
    return this;
  }

  /**
   * Returns the socket file descriptor.
   *
   * @return the file descriptor
   */
  public int fileDescriptor() {
    return fileDescriptor;
  }

  /**
   * Returns the raw epoll event mask.
   *
   * @return the event mask
   */
  public int eventMask() {
    return eventMask;
  }

  /**
   * Returns whether the socket is readable.
   *
   * @return true if EPOLLIN is set
   */
  public boolean isReadable() {
    return (eventMask & READABLE) != 0;
  }

  /**
   * Returns whether the socket is writable.
   *
   * @return true if EPOLLOUT is set
   */
  public boolean isWritable() {
    return (eventMask & WRITABLE) != 0;
  }

  /**
   * Returns whether the socket has an error.
   *
   * @return true if EPOLLERR is set
   */
  public boolean isError() {
    return (eventMask & ERROR) != 0;
  }

  /**
   * Returns whether the remote side hung up.
   *
   * @return true if EPOLLHUP is set
   */
  public boolean isHangup() {
    return (eventMask & HANGUP) != 0;
  }

  @Override
  public long timestampNanos() {
    return timestampNanos;
  }

  @Override
  public String toString() {
    return "SocketEvent{fd=" + fileDescriptor + ", mask=0x" + Integer.toHexString(eventMask) + "}";
  }
}
