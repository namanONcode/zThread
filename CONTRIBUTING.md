# Contributing to zThread

Thank you for your interest in contributing to zThread! This document provides guidelines and instructions for contributing.

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How to Contribute

### Reporting Bugs

Before creating a bug report, please check existing issues to avoid duplicates. When filing a bug report, include:

- A clear and descriptive title
- Steps to reproduce the behavior
- Expected behavior vs actual behavior
- Your environment (Java version, Linux distribution, kernel version)
- Relevant log output

### Suggesting Features

Feature requests are welcome. Please provide:

- A clear description of the feature
- The motivation and use case
- Any alternative solutions you've considered

### Pull Requests

1. Fork the repository and create your branch from `main`
2. Follow the existing code style (Google Java Style, enforced by Spotless)
3. Add tests for any new functionality
4. Ensure all tests pass: `./mvnw clean verify`
5. Run quality checks: `./mvnw verify -Pquality`
6. Update documentation as needed
7. Write a clear PR description

## Development Setup

### Prerequisites

- Java 25 or later
- Linux (Ubuntu, Fedora, Arch, Debian, or RHEL)
- Git

### Building

```bash
# Clone the repository
git clone https://github.com/namanoncode/zThread.git
cd zThread

# Build (uses Maven Wrapper, no Maven installation needed)
./mvnw clean verify

# Run with quality checks
./mvnw clean verify -Pquality
```

### Running Tests

```bash
# Unit tests only
./mvnw test

# Integration tests (requires Linux)
./mvnw verify -pl zthread-linux

# All tests with coverage
./mvnw verify
```

## Code Style

- Follow Google Java Style Guide
- Spotless is configured to auto-format: `./mvnw spotless:apply`
- Every public class and method must have Javadoc
- No magic numbers — use named constants
- Prefer immutability
- Use composition over inheritance

## License

By contributing, you agree that your contributions will be licensed under the Apache License, Version 2.0.
