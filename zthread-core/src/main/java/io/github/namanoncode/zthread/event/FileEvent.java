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
 * Event fired when a file system change is detected via inotify.
 *
 * @see ZEvent
 */
public final class FileEvent implements ZEvent {

  /** File was accessed. */
  public static final int ACCESS = 0x00000001;

  /** File was modified. */
  public static final int MODIFY = 0x00000002;

  /** Metadata changed. */
  public static final int ATTRIB = 0x00000004;

  /** Writable file was closed. */
  public static final int CLOSE_WRITE = 0x00000008;

  /** Read-only file was closed. */
  public static final int CLOSE_NOWRITE = 0x00000010;

  /** File was opened. */
  public static final int OPEN = 0x00000020;

  /** File was moved from watched directory. */
  public static final int MOVED_FROM = 0x00000040;

  /** File was moved to watched directory. */
  public static final int MOVED_TO = 0x00000080;

  /** File was created in watched directory. */
  public static final int CREATE = 0x00000100;

  /** File was deleted from watched directory. */
  public static final int DELETE = 0x00000200;

  /** Watched file/directory was deleted. */
  public static final int DELETE_SELF = 0x00000400;

  /** Watched file/directory was moved. */
  public static final int MOVE_SELF = 0x00000800;

  private int watchDescriptor;
  private int eventMask;
  private int cookie;
  private String name;
  private long timestampNanos;


  /**
   * Resets this event with new values.
   *
   * @param wd the watch descriptor
   * @param mask the inotify event mask
   * @param cookie the cookie for correlating move events
   * @param name the file name (may be null for the watched directory itself)
   * @return this event for chaining
   */
  public FileEvent reset(int wd, int mask, int cookie, String name) {
    this.watchDescriptor = wd;
    this.eventMask = mask;
    this.cookie = cookie;
    this.name = name;
    this.timestampNanos = System.nanoTime();
    return this;
  }

  /**
   * Returns the watch descriptor.
   *
   * @return the watch descriptor
   */
  public int watchDescriptor() {
    return watchDescriptor;
  }

  /**
   * Returns the inotify event mask.
   *
   * @return the event mask
   */
  public int eventMask() {
    return eventMask;
  }

  /**
   * Returns the cookie for correlating MOVED_FROM/MOVED_TO pairs.
   *
   * @return the cookie value
   */
  public int cookie() {
    return cookie;
  }

  /**
   * Returns the name of the file within the watched directory.
   *
   * @return the file name, or null if the event applies to the watched path itself
   */
  public String name() {
    return name;
  }

  /**
   * Returns whether a file was created.
   *
   * @return true if IN_CREATE is set
   */
  public boolean isCreate() {
    return (eventMask & CREATE) != 0;
  }

  /**
   * Returns whether a file was modified.
   *
   * @return true if IN_MODIFY is set
   */
  public boolean isModify() {
    return (eventMask & MODIFY) != 0;
  }

  /**
   * Returns whether a file was deleted.
   *
   * @return true if IN_DELETE is set
   */
  public boolean isDelete() {
    return (eventMask & DELETE) != 0;
  }

  @Override
  public long timestampNanos() {
    return timestampNanos;
  }

  @Override
  public String toString() {
    return "FileEvent{wd=" + watchDescriptor + ", mask=0x" + Integer.toHexString(eventMask)
        + ", name=" + name + "}";
  }
}
