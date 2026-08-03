# Glossary

Definitions of important technical terms used throughout the zThread project.

### `epoll`
A Linux kernel system call for scalable I/O event notification. It allows a single thread to monitor multiple file descriptors to see if I/O is possible on any of them. zThread uses it to park the event loop thread efficiently.

### `eventfd`
A Linux kernel mechanism used to create a file descriptor that can be used for event wait/notify operations between user-space applications. zThread uses it to wake up the event loop from `epoll_wait` when a new event is added to the ring buffer.

### FFM API (Foreign Function & Memory API)
A Java API (JEP 454) that enables Java programs to interoperate with code and data outside the Java runtime. It allows zThread to allocate native memory and call C functions (like `epoll_create`) without the overhead of JNI.

### JNI (Java Native Interface)
The older, legacy method for Java to call native code. zThread deliberately avoids JNI in favor of FFM for better performance and safety.

### MPSC (Multi-Producer, Single-Consumer)
A concurrent queue architecture where many threads can insert items (producers), but only one thread reads items (consumer). zThread uses a lock-free MPSC ring buffer to collect events from application threads before the single event loop thread dispatches them.

### Project Reactor
A fourth-generation reactive library for building non-blocking applications on the JVM based on the Reactive Streams Specification. `zthread-reactor` provides a bridge to this ecosystem.

### Ring Buffer
A fixed-size buffer that wraps around when it reaches the end. zThread uses it because it allows for extremely fast, lock-free insertions and prevents object allocation overhead during event dispatching.
