# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.0   | ✅        |

## Reporting a Vulnerability

If you discover a security vulnerability in zThread, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, please email the maintainer directly or use GitHub's private vulnerability reporting feature.

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### Response Timeline

- **Acknowledgment:** Within 48 hours
- **Assessment:** Within 1 week
- **Fix release:** As soon as practical, typically within 2 weeks

## Security Considerations

zThread uses the Java Foreign Function & Memory API to interact with Linux kernel syscalls directly. The following security practices are enforced:

- All native memory allocations are scoped to `Arena` instances with deterministic cleanup
- File descriptors are tracked and closed via `AutoCloseable` patterns
- Input validation is performed before passing data to native functions
- No user-controlled data is passed to native functions without sanitization
- The `--enable-native-access` flag is required at runtime, providing an explicit opt-in
