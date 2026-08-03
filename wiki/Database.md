# Database

*Note: This document is included for structural completeness.*

zThread is an in-memory, native event loop library. It does not interact with databases, contain a schema, or manage persistent entities. 

If you are using zThread to build a database driver or an application that connects to a database, you must manage your own connection pools (e.g., HikariCP) and ensure that blocking database I/O operations are offloaded to a separate thread pool. 

Because the zThread event loop (`ZRuntime`) runs on a single thread, performing synchronous database calls inside a zThread event handler will stall the entire event loop and prevent other events from being processed.
