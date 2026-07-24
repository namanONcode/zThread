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

import io.github.namanoncode.zthread.NativePoller;
import io.github.namanoncode.zthread.exception.NativeException;
import io.github.namanoncode.zthread.linux.native_.LinuxConstants;
import io.github.namanoncode.zthread.linux.native_.LinuxSyscalls;
import io.github.namanoncode.zthread.linux.native_.StructLayouts;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux epoll-based implementation of {@link NativePoller}.
 *
 * <p>Manages an epoll file descriptor and a pre-allocated buffer for epoll_event structs.
 * The buffer is allocated in an arena that lives for the lifetime of the poller.
 */
public final class LinuxNativePoller implements NativePoller {

  private static final Logger LOG = LoggerFactory.getLogger(LinuxNativePoller.class);

  private final int epollFd;
  private final int maxEvents;
  private final Arena arena;
  private final MemorySegment eventsBuffer;
  private final MemorySegment singleEventBuffer;
  private volatile boolean closed;

  /**
   * Creates a new epoll-based poller.
   *
   * @param maxEvents the maximum events per poll call
   */
  public LinuxNativePoller(int maxEvents) {
    this.maxEvents = maxEvents;
    this.arena = Arena.ofShared();

    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      this.epollFd = (int) LinuxSyscalls.EPOLL_CREATE1.invokeExact(
          captureState, LinuxConstants.EPOLL_CLOEXEC);
      if (epollFd < 0) {
        throw new NativeException("epoll_create1 failed",
            LinuxSyscalls.extractErrno(captureState));
      }
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("epoll_create1 invocation failed", -1, e);
    }

    this.eventsBuffer = arena.allocate(
        StructLayouts.EPOLL_EVENT_SIZE * maxEvents,
        StructLayouts.EPOLL_EVENT.byteAlignment());
    this.singleEventBuffer = arena.allocate(StructLayouts.EPOLL_EVENT_SIZE,
        StructLayouts.EPOLL_EVENT.byteAlignment());

    LOG.debug("Created epoll fd={}, maxEvents={}", epollFd, maxEvents);
  }

  @Override
  public int poll(int timeoutMillis) {
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int result = (int) LinuxSyscalls.EPOLL_WAIT.invokeExact(
          captureState, epollFd, eventsBuffer, maxEvents, timeoutMillis);
      if (result < 0) {
        int errno = LinuxSyscalls.extractErrno(captureState);
        if (errno == 4) { // EINTR
          return 0;
        }
        throw new NativeException("epoll_wait failed", errno);
      }
      return result;
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("epoll_wait invocation failed", -1, e);
    }
  }

  @Override
  public int getEvents(int index) {
    return StructLayouts.readEpollEvents(eventsBuffer,
        (long) index * StructLayouts.EPOLL_EVENT_SIZE);
  }

  @Override
  public long getUserData(int index) {
    return StructLayouts.readEpollData(eventsBuffer,
        (long) index * StructLayouts.EPOLL_EVENT_SIZE);
  }

  @Override
  public void addFd(int fd, int events, long userData) {
    epollCtl(LinuxConstants.EPOLL_CTL_ADD, fd, events, userData);
  }

  @Override
  public void modifyFd(int fd, int events, long userData) {
    epollCtl(LinuxConstants.EPOLL_CTL_MOD, fd, events, userData);
  }

  @Override
  public void removeFd(int fd) {
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int result = (int) LinuxSyscalls.EPOLL_CTL.invokeExact(
          captureState, epollFd, LinuxConstants.EPOLL_CTL_DEL, fd,
          MemorySegment.NULL);
      if (result < 0) {
        int errno = LinuxSyscalls.extractErrno(captureState);
        LOG.warn("epoll_ctl DEL fd={} failed, errno={}", fd, errno);
      }
    } catch (Throwable e) {
      LOG.error("epoll_ctl DEL invocation failed for fd={}", fd, e);
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int res = (int) LinuxSyscalls.CLOSE.invokeExact(captureState, epollFd);
    } catch (Throwable e) {
      LOG.error("Failed to close epoll fd={}", epollFd, e);
    }
    arena.close();
    LOG.debug("Closed epoll fd={}", epollFd);
  }

  /**
   * Returns the epoll file descriptor.
   *
   * @return the epoll fd
   */
  public int epollFd() {
    return epollFd;
  }

  private void epollCtl(int op, int fd, int events, long userData) {
    try {
      StructLayouts.writeEpollEvent(singleEventBuffer, 0, events, userData);
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int result = (int) LinuxSyscalls.EPOLL_CTL.invokeExact(
          captureState, epollFd, op, fd, singleEventBuffer);
      if (result < 0) {
        throw new NativeException(
            "epoll_ctl op=" + op + " fd=" + fd + " failed",
            LinuxSyscalls.extractErrno(captureState));
      }
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("epoll_ctl invocation failed", -1, e);
    }
  }
}
