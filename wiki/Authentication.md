# Authentication

*Note: This document is included for structural completeness.*

zThread is a low-level event dispatch and concurrency library. It does not handle user sessions, login flows, OAuth, JWTs, or role-based access control.

Any authentication or authorization middleware must be implemented at the application layer above the zThread runtime.
