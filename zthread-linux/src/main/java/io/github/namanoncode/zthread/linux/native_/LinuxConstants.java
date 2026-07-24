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

/**
 * Linux kernel constants for epoll, eventfd, timerfd, signalfd, inotify, and sockets.
 *
 * <p>Values are taken directly from Linux kernel headers on x86_64.
 */
public final class LinuxConstants {

  private LinuxConstants() {
    throw new AssertionError("Utility class");
  }

  // ---- epoll ----
  public static final int EPOLLIN = 0x001;
  public static final int EPOLLPRI = 0x002;
  public static final int EPOLLOUT = 0x004;
  public static final int EPOLLERR = 0x008;
  public static final int EPOLLHUP = 0x010;
  public static final int EPOLLRDHUP = 0x2000;
  public static final int EPOLLET = 1 << 31;
  public static final int EPOLLONESHOT = 1 << 30;
  public static final int EPOLL_CLOEXEC = 0x80000; // 02000000 octal
  public static final int EPOLL_CTL_ADD = 1;
  public static final int EPOLL_CTL_DEL = 2;
  public static final int EPOLL_CTL_MOD = 3;

  // ---- eventfd ----
  public static final int EFD_CLOEXEC = 0x80000;
  public static final int EFD_NONBLOCK = 0x800;
  public static final int EFD_SEMAPHORE = 1;

  // ---- timerfd ----
  public static final int TFD_CLOEXEC = 0x80000;
  public static final int TFD_NONBLOCK = 0x800;
  public static final int TFD_TIMER_ABSTIME = 1;
  public static final int CLOCK_REALTIME = 0;
  public static final int CLOCK_MONOTONIC = 1;

  // ---- signalfd ----
  public static final int SFD_CLOEXEC = 0x80000;
  public static final int SFD_NONBLOCK = 0x800;
  public static final int SIG_BLOCK = 0;
  public static final int SIG_UNBLOCK = 1;
  public static final int SIG_SETMASK = 2;

  // ---- Signals ----
  public static final int SIGHUP = 1;
  public static final int SIGINT = 2;
  public static final int SIGQUIT = 3;
  public static final int SIGTERM = 15;
  public static final int SIGUSR1 = 10;
  public static final int SIGUSR2 = 12;
  public static final int SIGCHLD = 17;

  /** Size of sigset_t on x86_64 Linux (128 bytes = 1024 bits). */
  public static final int SIGSET_SIZE = 128;

  // ---- inotify ----
  public static final int IN_CLOEXEC = 0x80000;
  public static final int IN_NONBLOCK = 0x800;
  public static final int IN_ACCESS = 0x00000001;
  public static final int IN_MODIFY = 0x00000002;
  public static final int IN_ATTRIB = 0x00000004;
  public static final int IN_CLOSE_WRITE = 0x00000008;
  public static final int IN_CLOSE_NOWRITE = 0x00000010;
  public static final int IN_OPEN = 0x00000020;
  public static final int IN_MOVED_FROM = 0x00000040;
  public static final int IN_MOVED_TO = 0x00000080;
  public static final int IN_CREATE = 0x00000100;
  public static final int IN_DELETE = 0x00000200;
  public static final int IN_DELETE_SELF = 0x00000400;
  public static final int IN_MOVE_SELF = 0x00000800;
  public static final int IN_ALL_EVENTS = 0x00000FFF;

  /** Size of inotify_event base struct (without variable-length name). */
  public static final int INOTIFY_EVENT_BASE_SIZE = 16;

  // ---- Socket ----
  public static final int AF_INET = 2;
  public static final int SOCK_STREAM = 1;
  public static final int SOCK_NONBLOCK = 0x800;
  public static final int SOCK_CLOEXEC = 0x80000;
  public static final int SOL_SOCKET = 1;
  public static final int SO_REUSEADDR = 2;
  public static final int SO_REUSEPORT = 15;

  // ---- fcntl ----
  public static final int F_GETFL = 3;
  public static final int F_SETFL = 4;
  public static final int O_NONBLOCK = 0x800;
  public static final int O_CLOEXEC = 0x80000;

  // ---- signalfd_siginfo offsets (128 bytes total) ----
  public static final long SIGINFO_SIGNO_OFFSET = 0;
  public static final long SIGINFO_PID_OFFSET = 12;
  public static final long SIGINFO_UID_OFFSET = 16;
  public static final long SIGINFO_SIZE = 128;
}
