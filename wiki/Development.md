# Development Workflow

This page outlines the local development setup, coding standards, and common tasks for contributors working on the zThread codebase.

## Prerequisites
* **OS:** A Linux environment (natively or via WSL2) is required because the library depends on kernel-specific features (`epoll`, `eventfd`).
* **Java:** JDK 25.
* **Maven:** Included via the Maven Wrapper (`./mvnw`).

## Coding Standards

The project enforces strict code quality and formatting rules. Before submitting a pull request, ensure your code passes the quality profile.

### Linting and Formatting

Run the quality check profile:
```bash
./mvnw clean verify -Pquality
```

This runs the following plugins:
1. **Spotless:** Enforces Google Java Format and prepends the Apache 2.0 license header.
2. **Checkstyle:** Validates naming conventions, import ordering, and Javadoc presence.
3. **PMD:** Scans for potential bugs, dead code, and suboptimal performance patterns.
4. **SpotBugs:** Analyzes bytecode for common bug patterns.

If Spotless fails due to formatting, you can automatically fix it with:
```bash
./mvnw spotless:apply
```

## Local Workflow

1. **Build and test:**
   ```bash
   ./mvnw clean verify --enable-native-access=ALL-UNNAMED
   ```

2. **Run benchmarks:**
   ```bash
   cd zthread-benchmark
   ../mvnw clean package
   java --enable-native-access=ALL-UNNAMED -jar target/benchmarks.jar
   ```

## Debugging

Because zThread utilizes the FFM API, segmentation faults can occur if off-heap memory is mismanaged or accessed after it has been freed. 

To debug native memory issues, you can run the JVM with:
```bash
java -XX:NativeMemoryTracking=detail --enable-native-access=ALL-UNNAMED ...
```
For deep native stack traces, attach `gdb` to the Java process.
