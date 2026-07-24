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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * FFM API downcall handles for Linux system calls.
 *
 * <p>All method handles are resolved lazily on first access and cached. The handles
 * use {@link Linker.Option#captureCallState(String...)} to capture {@code errno}
 * after each call.
 *
 * <p>Thread safety: This class is thread-safe. Method handles are immutable once created.
 */
public final class LinuxSyscalls {

  private static final Linker LINKER = Linker.nativeLinker();
  private static final SymbolLookup STDLIB = LINKER.defaultLookup();
  private static final Linker.Option CAPTURE_ERRNO =
      Linker.Option.captureCallState("errno");
  private static final MemoryLayout CAPTURE_STATE_LAYOUT =
      Linker.Option.captureStateLayout();

  private LinuxSyscalls() {
    throw new AssertionError("Utility class — do not instantiate");
  }

  // ---- epoll ----

  /** {@code int epoll_create1(int flags)} */
  public static final MethodHandle EPOLL_CREATE1 = downcall("epoll_create1",
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  /** {@code int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event)} */
  public static final MethodHandle EPOLL_CTL = downcall("epoll_ctl",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
          ValueLayout.ADDRESS));

  /** {@code int epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout)} */
  public static final MethodHandle EPOLL_WAIT = downcall("epoll_wait",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT));

  // ---- eventfd ----

  /** {@code int eventfd(unsigned int initval, int flags)} */
  public static final MethodHandle EVENTFD = downcall("eventfd",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  // ---- timerfd ----

  /** {@code int timerfd_create(int clockid, int flags)} */
  public static final MethodHandle TIMERFD_CREATE = downcall("timerfd_create",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  /** {@code int timerfd_settime(int fd, int flags, struct itimerspec *new, struct itimerspec *old)} */
  public static final MethodHandle TIMERFD_SETTIME = downcall("timerfd_settime",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
          ValueLayout.ADDRESS, ValueLayout.ADDRESS));

  // ---- signalfd ----

  /** {@code int signalfd(int fd, const sigset_t *mask, int flags)} */
  public static final MethodHandle SIGNALFD = downcall("signalfd",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

  // ---- inotify ----

  /** {@code int inotify_init1(int flags)} */
  public static final MethodHandle INOTIFY_INIT1 = downcall("inotify_init1",
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  /** {@code int inotify_add_watch(int fd, const char *pathname, uint32_t mask)} */
  public static final MethodHandle INOTIFY_ADD_WATCH = downcall("inotify_add_watch",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

  // ---- POSIX I/O ----

  /** {@code int close(int fd)} */
  public static final MethodHandle CLOSE = downcall("close",
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  /** {@code ssize_t read(int fd, void *buf, size_t count)} */
  public static final MethodHandle READ = downcall("read",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

  /** {@code ssize_t write(int fd, const void *buf, size_t count)} */
  public static final MethodHandle WRITE = downcall("write",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

  // ---- Socket ----

  /** {@code int socket(int domain, int type, int protocol)} */
  public static final MethodHandle SOCKET = downcall("socket",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  /** {@code int bind(int sockfd, struct sockaddr *addr, socklen_t addrlen)} */
  public static final MethodHandle BIND = downcall("bind",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

  /** {@code int listen(int sockfd, int backlog)} */
  public static final MethodHandle LISTEN = downcall("listen",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  /** {@code int accept4(int sockfd, struct sockaddr *addr, socklen_t *addrlen, int flags)} */
  public static final MethodHandle ACCEPT4 = downcall("accept4",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
          ValueLayout.JAVA_INT));

  /** {@code ssize_t recv(int sockfd, void *buf, size_t len, int flags)} */
  public static final MethodHandle RECV = downcall("recv",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT));

  /** {@code ssize_t send(int sockfd, const void *buf, size_t len, int flags)} */
  public static final MethodHandle SEND = downcall("send",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
          ValueLayout.JAVA_INT));

  /** {@code int setsockopt(int sockfd, int level, int optname, void *optval, socklen_t optlen)} */
  public static final MethodHandle SETSOCKOPT = downcall("setsockopt",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
          ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

  /** {@code int fcntl(int fd, int cmd, int arg)} */
  public static final MethodHandle FCNTL = downcall("fcntl",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

  // ---- Signal ----

  /** {@code int sigemptyset(sigset_t *set)} */
  public static final MethodHandle SIGEMPTYSET = downcall("sigemptyset",
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

  /** {@code int sigaddset(sigset_t *set, int signum)} */
  public static final MethodHandle SIGADDSET = downcall("sigaddset",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

  /** {@code int sigprocmask(int how, const sigset_t *set, sigset_t *oldset)} */
  public static final MethodHandle SIGPROCMASK = downcall("sigprocmask",
      FunctionDescriptor.of(ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

  /**
   * Returns the capture state layout for errno extraction.
   *
   * @return the capture state layout
   */
  public static MemoryLayout captureStateLayout() {
    return CAPTURE_STATE_LAYOUT;
  }

  /**
   * Extracts the errno value from a captured call state segment.
   *
   * @param capturedState the captured state from a downcall
   * @return the errno value
   */
  public static int extractErrno(MemorySegment capturedState) {
    return (int) CAPTURE_STATE_LAYOUT
        .varHandle(MemoryLayout.PathElement.groupElement("errno"))
        .get(capturedState, 0L);
  }

  /**
   * Allocates a capture state segment for use with errno-capturing downcalls.
   *
   * @param arena the arena for allocation
   * @return a new capture state segment
   */
  public static MemorySegment allocateCaptureState(Arena arena) {
    return arena.allocate(CAPTURE_STATE_LAYOUT);
  }

  private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
    MemorySegment symbol = STDLIB.find(name)
        .or(() -> SymbolLookup.loaderLookup().find(name))
        .orElseThrow(() -> new UnsatisfiedLinkError(
            "Cannot find native symbol: " + name));
    return LINKER.downcallHandle(symbol, descriptor, CAPTURE_ERRNO);
  }
}
