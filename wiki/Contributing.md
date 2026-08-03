# Contributing

We welcome contributions to zThread! This guide covers how to submit code, report bugs, and align with our development standards.

## Reporting Bugs

Please use the GitHub Issue tracker to report bugs. Include:
* Your OS version and kernel.
* Your JDK version.
* A clear, reproducible code example if possible.

## Pull Request Process

1. **Fork and Branch:** Create a fork of the repository and create a feature branch (`feature/your-feature` or `fix/issue-description`).
2. **Write Tests:** Ensure your new feature is covered by unit tests. If you modified native interactions, add an integration test.
3. **Format Code:** Run `./mvnw spotless:apply` to ensure your code matches the Google Java Format and includes the Apache 2.0 license header.
4. **Pass Checks:** Run `./mvnw clean verify -Pquality` to verify that Checkstyle, PMD, and SpotBugs pass.
5. **Submit PR:** Open a Pull Request against the `main` branch. 

## Commit Conventions

While not strictly enforced via tooling, we appreciate clean, descriptive commit messages.

* Use the imperative mood: "Fix bug in ring buffer" rather than "Fixed bug".
* If your commit resolves an issue, include `Fixes #123` in the description.

## Development Environment Setup

See [Development Workflow](Development) for details on the required JDK, Maven commands, and debugging tools.
