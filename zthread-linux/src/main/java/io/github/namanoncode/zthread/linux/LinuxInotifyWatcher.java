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

import io.github.namanoncode.zthread.EventDispatcher;
import io.github.namanoncode.zthread.event.FileEvent;
import io.github.namanoncode.zthread.exception.NativeException;
import io.github.namanoncode.zthread.handler.HandlerRegistration;
import io.github.namanoncode.zthread.linux.native_.LinuxConstants;
import io.github.namanoncode.zthread.linux.native_.LinuxSyscalls;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux inotify-based file system watcher.
 */
@SuppressWarnings("PMD")
public final class LinuxInotifyWatcher implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(LinuxInotifyWatcher.class);
  private static final int BUFFER_SIZE = 4096;

  private final LinuxEventLoop eventLoop;
  private final EventDispatcher dispatcher;
  private final Arena arena;
  private final int inotifyFd;
  private final MemorySegment readBuffer;
  private final ConcurrentHashMap<Integer, WatchRegistration> watches = new ConcurrentHashMap<>();

  public LinuxInotifyWatcher(LinuxEventLoop eventLoop, EventDispatcher dispatcher) {
    this.eventLoop = eventLoop;
    this.dispatcher = dispatcher;
    this.arena = Arena.ofShared();
    this.readBuffer = arena.allocate(BUFFER_SIZE);

    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      this.inotifyFd = (int) LinuxSyscalls.INOTIFY_INIT1.invokeExact(
          captureState, LinuxConstants.IN_NONBLOCK | LinuxConstants.IN_CLOEXEC);
      if (inotifyFd < 0) {
        throw new NativeException("inotify_init1 failed",
            LinuxSyscalls.extractErrno(captureState));
      }
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("inotify_init1 invocation failed", -1, e);
    }

    eventLoop.registerFd(inotifyFd, LinuxConstants.EPOLLIN, LinuxEventLoop.TAG_INOTIFY,
        this::handleInotifyEvent);
    LOG.debug("Created inotify fd={}", inotifyFd);
  }

  public HandlerRegistration watch(Path path) {
    return watch(path, LinuxConstants.IN_ALL_EVENTS);
  }

  public HandlerRegistration watch(Path path, int mask) {
    String pathStr = path.toAbsolutePath().toString();
    try {
      MemorySegment pathSegment = arena.allocateFrom(pathStr);
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int wd = (int) LinuxSyscalls.INOTIFY_ADD_WATCH.invokeExact(
          captureState, inotifyFd, pathSegment, mask);
      if (wd < 0) {
        throw new NativeException("inotify_add_watch failed for " + pathStr,
            LinuxSyscalls.extractErrno(captureState));
      }
      WatchRegistration reg = new WatchRegistration(wd, pathStr);
      watches.put(wd, reg);
      LOG.debug("Added inotify watch wd={} for path={}", wd, pathStr);
      return reg;
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("inotify_add_watch invocation failed", -1, e);
    }
  }

  @Override
  public void close() {
    eventLoop.unregisterFd(inotifyFd);
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      int ignored = (int) LinuxSyscalls.CLOSE.invokeExact(captureState, inotifyFd);
    } catch (Throwable e) {
      LOG.error("Failed to close inotify fd={}", inotifyFd, e);
    }
    arena.close();
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private void handleInotifyEvent(int fd, int epollEvents) {
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(arena);
      long bytesRead = (long) LinuxSyscalls.READ.invokeExact(captureState, inotifyFd, readBuffer, (long) BUFFER_SIZE);

      if (bytesRead <= 0) {
        return;
      }

      long offset = 0;
      while (offset < bytesRead) {
        int wd = readBuffer.get(ValueLayout.JAVA_INT, offset);
        int mask = readBuffer.get(ValueLayout.JAVA_INT, offset + 4);
        int cookie = readBuffer.get(ValueLayout.JAVA_INT, offset + 8);
        int len = readBuffer.get(ValueLayout.JAVA_INT, offset + 12);

        String name = null;
        if (len > 0) {
          name = readBuffer.getString(offset + 16);
        }

        WatchRegistration reg = watches.get(wd);
        if (reg != null && !reg.isCancelled()) {
          FileEvent event = new FileEvent().reset(wd, mask, cookie, name);
          dispatcher.dispatch(event);
        }

        offset += LinuxConstants.INOTIFY_EVENT_BASE_SIZE + len;
      }
    } catch (Throwable e) {
      LOG.error("Failed to read from inotify fd={}", inotifyFd, e);
    }
  }

  private class WatchRegistration implements HandlerRegistration {
    private final int wd;
    private final String path;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    WatchRegistration(int wd, String path) {
      this.wd = wd;
      this.path = path;
    }

    public String path() {
      return path;
    }

    @Override
    public void cancel() {
      if (cancelled.compareAndSet(false, true)) {
        watches.remove(wd);
        // We could call inotify_rm_watch here, but closing the fd cleans it up eventually,
        // and we might need to if we want immediate cleanup. For now, just ignoring events.
      }
    }

    @Override
    public boolean isCancelled() {
      return cancelled.get();
    }
  }
}
