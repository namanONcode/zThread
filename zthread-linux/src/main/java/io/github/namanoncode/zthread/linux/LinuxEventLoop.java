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
import io.github.namanoncode.zthread.EventLoop;
import io.github.namanoncode.zthread.ZRuntimeConfig;
import io.github.namanoncode.zthread.event.ShutdownEvent;
import io.github.namanoncode.zthread.exception.NativeException;
import io.github.namanoncode.zthread.linux.native_.LinuxConstants;
import io.github.namanoncode.zthread.linux.native_.LinuxSyscalls;
import io.github.namanoncode.zthread.metrics.RuntimeMetrics;
import io.github.namanoncode.zthread.util.MpscRingBuffer;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux-native event loop implementation.
 *
 * <p>The event loop runs on a single dedicated thread and blocks inside
 * {@code epoll_wait} when no work is available. Producer threads wake the loop
 * by writing to an eventfd. Custom events are passed through a lock-free
 * {@link MpscRingBuffer}.
 *
 * <h2>File Descriptor Identification</h2>
 * <p>Each registered fd gets a unique long identifier stored as epoll user data.
 * The event loop uses this to determine the fd type (wakeup eventfd, timer,
 * socket, inotify, signalfd) and dispatch accordingly.
 *
 * <p>The identifier encoding uses the upper 8 bits as a type tag:
 * <ul>
 *   <li>{@code 0x01_xxxxxxxx} — wakeup eventfd</li>
 *   <li>{@code 0x02_xxxxxxxx} — timer fd</li>
 *   <li>{@code 0x03_xxxxxxxx} — socket fd</li>
 *   <li>{@code 0x04_xxxxxxxx} — inotify fd</li>
 *   <li>{@code 0x05_xxxxxxxx} — signalfd</li>
 * </ul>
 */
@SuppressWarnings("PMD")
public final class LinuxEventLoop implements EventLoop {

  private static final Logger LOG = LoggerFactory.getLogger(LinuxEventLoop.class);

  // Type tags for fd identification in epoll user data
  static final long TAG_WAKEUP = 0x01_00000000L;
  static final long TAG_TIMER = 0x02_00000000L;
  static final long TAG_SOCKET = 0x03_00000000L;
  static final long TAG_INOTIFY = 0x04_00000000L;
  static final long TAG_SIGNAL = 0x05_00000000L;
  static final long TAG_MASK = 0xFF_00000000L;
  static final long FD_MASK = 0x00_FFFFFFFFL;

  private final ZRuntimeConfig config;
  private final LinuxNativePoller poller;
  private final EventDispatcher dispatcher;
  private final RuntimeMetrics metrics;
  private final MpscRingBuffer ringBuffer;
  private final Map<Integer, Runnable> timerCallbacks = new ConcurrentHashMap<>();
  private final Map<Integer, FdHandler> fdHandlers = new ConcurrentHashMap<>();

  private final int wakeupFd;
  private final Arena loopArena;
  private final MemorySegment wakeupBuf;
  private final MemorySegment wakeupCaptureState;
  private final AtomicBoolean inEpollWait = new AtomicBoolean(false);

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
  private volatile Thread loopThread;

  /**
   * Creates a new Linux event loop.
   *
   * @param config the runtime configuration
   * @param dispatcher the event dispatcher
   * @param metrics the metrics collector
   */
  public LinuxEventLoop(ZRuntimeConfig config, EventDispatcher dispatcher,
      RuntimeMetrics metrics) {
    this.config = config;
    this.dispatcher = dispatcher;
    this.metrics = metrics;
    this.ringBuffer = new MpscRingBuffer(config.bufferSize());
    this.loopArena = Arena.ofShared();

    this.poller = new LinuxNativePoller(config.maxEventsPerPoll());

    // Create wakeup eventfd
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(loopArena);
      this.wakeupFd = (int) LinuxSyscalls.EVENTFD.invokeExact(
          captureState, 0, LinuxConstants.EFD_NONBLOCK | LinuxConstants.EFD_CLOEXEC);
      if (wakeupFd < 0) {
        throw new NativeException("eventfd creation failed",
            LinuxSyscalls.extractErrno(captureState));
      }
    } catch (NativeException e) {
      throw e;
    } catch (Throwable e) {
      throw new NativeException("eventfd invocation failed", -1, e);
    }

    // Pre-allocate wakeup buffers to eliminate FFM allocation overhead per event
    this.wakeupBuf = loopArena.allocate(ValueLayout.JAVA_LONG);
    this.wakeupBuf.set(ValueLayout.JAVA_LONG, 0, 1L);
    this.wakeupCaptureState = LinuxSyscalls.allocateCaptureState(loopArena);

    // Register wakeup fd with epoll
    poller.addFd(wakeupFd, LinuxConstants.EPOLLIN | LinuxConstants.EPOLLET,
        TAG_WAKEUP | (wakeupFd & FD_MASK));

    LOG.debug("Event loop created: wakeupFd={}, bufferSize={}", wakeupFd, config.bufferSize());
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    Thread thread = Thread.ofPlatform()
        .name(config.threadName())
        .daemon(false)
        .start(this::runLoop);
    this.loopThread = thread;
    LOG.info("Event loop started on thread '{}'", config.threadName());
  }

  @Override
  public void stop() {
    if (!shuttingDown.compareAndSet(false, true)) {
      return;
    }
    LOG.info("Event loop shutdown initiated");
    forceWakeup();
  }

  @Override
  public boolean isRunning() {
    return running.get() && !shuttingDown.get();
  }

  @Override
  public void wakeup() {
    if (inEpollWait.compareAndSet(true, false)) {
      forceWakeup();
    }
  }

  private void forceWakeup() {
    try {
      long bytesWritten = (long) LinuxSyscalls.WRITE.invokeExact(wakeupCaptureState, wakeupFd, wakeupBuf, 8L);
      metrics.incrementWakeups();
    } catch (Throwable e) {
      LOG.error("Failed to write to wakeup eventfd", e);
    }
  }

  @Override
  public void close() {
    stop();
    Thread t = loopThread;
    if (t != null) {
      try {
        t.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    closeFd(wakeupFd);
    poller.close();
    loopArena.close();
    LOG.info("Event loop closed");
  }

  /**
   * Registers a timer fd with the event loop.
   *
   * @param timerFd the timer file descriptor
   * @param callback the callback to invoke on expiration
   */
  public void registerTimer(int timerFd, Runnable callback) {
    timerCallbacks.put(timerFd, callback);
    poller.addFd(timerFd, LinuxConstants.EPOLLIN, TAG_TIMER | (timerFd & FD_MASK));
  }

  /**
   * Unregisters a timer fd.
   *
   * @param timerFd the timer file descriptor
   */
  public void unregisterTimer(int timerFd) {
    timerCallbacks.remove(timerFd);
    poller.removeFd(timerFd);
  }

  /**
   * Registers a generic fd handler.
   *
   * @param fd the file descriptor
   * @param events the epoll events to monitor
   * @param tag the type tag
   * @param handler the handler
   */
  public void registerFd(int fd, int events, long tag, FdHandler handler) {
    fdHandlers.put(fd, handler);
    poller.addFd(fd, events, tag | (fd & FD_MASK));
  }

  /**
   * Unregisters a fd handler.
   *
   * @param fd the file descriptor
   */
  public void unregisterFd(int fd) {
    fdHandlers.remove(fd);
    poller.removeFd(fd);
  }

  /**
   * Posts an event to the ring buffer and wakes the loop.
   *
   * @param event the event to post
   * @return true if successfully enqueued
   */
  public boolean postToRingBuffer(Object event) {
    boolean success = ringBuffer.offer(event);
    if (success) {
      wakeup();
    }
    return success;
  }

  /**
   * Returns the ring buffer for custom events.
   *
   * @return the ring buffer
   */
  public MpscRingBuffer ringBuffer() {
    return ringBuffer;
  }

  /**
   * Returns the native poller.
   *
   * @return the poller
   */
  public LinuxNativePoller poller() {
    return poller;
  }

  private void runLoop() {
    LOG.debug("Event loop thread running");
    try {
      while (!shuttingDown.get()) {
        long loopStart = System.nanoTime();

        drainRingBuffer();

        int timeout = ringBuffer.isEmpty() ? -1 : 0;
        if (timeout == -1) {
          inEpollWait.set(true);
          if (!ringBuffer.isEmpty()) {
            inEpollWait.set(false);
            timeout = 0;
          }
        }

        int numEvents = poller.poll(timeout);
        inEpollWait.set(false);

        for (int i = 0; i < numEvents; i++) {
          long userData = poller.getUserData(i);
          int events = poller.getEvents(i);
          long tag = userData & TAG_MASK;
          int fd = (int) (userData & FD_MASK);

          processEvent(tag, fd, events);
        }

        drainRingBuffer();

        metrics.recordLoopLatency(System.nanoTime() - loopStart);
        metrics.updateQueueDepth(ringBuffer.size());
      }
    } catch (Exception e) {
      LOG.error("Event loop terminated with exception", e);
    } finally {
      dispatcher.dispatch(new ShutdownEvent());
      running.set(false);
      LOG.debug("Event loop thread exiting");
    }
  }

  private void processEvent(long tag, int fd, int events) {
    if (tag == TAG_WAKEUP) {
      drainEventfd(fd);
    } else if (tag == TAG_TIMER) {
      handleTimerEvent(fd);
    } else {
      FdHandler handler = fdHandlers.get(fd);
      if (handler != null) {
        try {
          handler.handleFdEvent(fd, events);
        } catch (Exception e) {
          LOG.error("Fd handler failed for fd={}", fd, e);
        }
      }
    }
  }

  private void handleTimerEvent(int fd) {
    // Read the timerfd to acknowledge the expiration
    try {
      MemorySegment buf = loopArena.allocate(ValueLayout.JAVA_LONG);
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(loopArena);
      long bytesRead = (long) LinuxSyscalls.READ.invokeExact(captureState, fd, buf, 8L);
    } catch (Throwable e) {
      LOG.error("Failed to read timerfd={}", fd, e);
    }

    Runnable callback = timerCallbacks.get(fd);
    if (callback != null) {
      long start = System.nanoTime();
      try {
        callback.run();
      } catch (Exception e) {
        LOG.error("Timer callback failed for fd=" + fd, e);
      }
      metrics.recordHandlerTime(System.nanoTime() - start);
      metrics.incrementEventsProcessed();
    }
  }

  private void drainEventfd(int fd) {
    try {
      MemorySegment buf = loopArena.allocate(ValueLayout.JAVA_LONG);
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(loopArena);
      long ignored = (long) LinuxSyscalls.READ.invokeExact(captureState, fd, buf, 8L);
    } catch (Throwable e) {
      LOG.error("Failed to drain eventfd={}", fd, e);
    }
  }

  private void drainRingBuffer() {
    Object value = ringBuffer.poll();
    while (value != null) {
      if (value instanceof io.github.namanoncode.zthread.event.ZEvent) {
        dispatcher.dispatch((io.github.namanoncode.zthread.event.ZEvent) value);
        metrics.incrementEventsProcessed();
      }
      value = ringBuffer.poll();
    }
  }

  private void closeFd(int fd) {
    try {
      MemorySegment captureState = LinuxSyscalls.allocateCaptureState(loopArena);
      int ignored = (int) LinuxSyscalls.CLOSE.invokeExact(captureState, fd);
    } catch (Throwable e) {
      LOG.error("Failed to close fd={}", fd, e);
    }
  }

  /**
   * Callback interface for file descriptor events.
   */
  @FunctionalInterface
  public interface FdHandler {
    /**
     * Handles an event on the given file descriptor.
     *
     * @param fd the file descriptor
     * @param events the epoll events mask
     */
    void handleFdEvent(int fd, int events);
  }
}
