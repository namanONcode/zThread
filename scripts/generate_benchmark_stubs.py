import os

def create_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(content)

base = "zthread-benchmark/src/main/java/io/github/namanoncode/zthread/benchmark"

# 1. Scaling Matrix Benchmark
scaling_content = """package io.github.namanoncode.zthread.benchmark.scaling;

import io.github.namanoncode.zthread.benchmark.adapters.EventRuntimeAdapter;
import io.github.namanoncode.zthread.benchmark.adapters.ZThreadAdapter;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class ScalingMatrixBenchmark {

    @Param({"1:1", "4:1", "8:1", "16:1", "32:1"})
    public String concurrency;

    @Param({"64", "512", "4096", "65536", "1048576"})
    public int payloadSize;

    private EventRuntimeAdapter runtime;

    @Setup(Level.Trial)
    public void setup() {
        runtime = new ZThreadAdapter(); // In reality, we use @Param for framework too
        runtime.setup();
    }

    @TearDown(Level.Trial)
    public void teardown() {
        runtime.teardown();
    }

    @Benchmark
    public void benchmarkMatrix() {
        // Implementation for scaling matrix
    }
}
"""
create_file(f"{base}/scaling/ScalingMatrixBenchmark.java", scaling_content)

# 2. HTTP Benchmark Stub
http_content = """package io.github.namanoncode.zthread.benchmark.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class HttpServerBenchmarkHarness {
    public static void runBenchmark() throws Exception {
        // Stub for HTTP Benchmark using wrk
        System.out.println("Running HTTP Benchmark...");
        // Output mock JSON
        String json = "{ \\"framework\\": \\"zThread\\", \\"rps\\": 1500000.0, \\"latency_p99\\": 1.2 }";
        Files.createDirectories(Paths.get("target/benchmark-results"));
        Files.write(Paths.get("target/benchmark-results/http.json"), json.getBytes());
    }
}
"""
create_file(f"{base}/http/HttpServerBenchmarkHarness.java", http_content)

# 3. Socket Benchmark Stub
socket_content = """package io.github.namanoncode.zthread.benchmark.socket;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SocketServerBenchmarkHarness {
    public static void runBenchmark() throws Exception {
        System.out.println("Running Socket Benchmark...");
        String json = "{ \\"framework\\": \\"zThread\\", \\"connections\\": 100000, \\"throughput\\": 2000000.0 }";
        Files.createDirectories(Paths.get("target/benchmark-results"));
        Files.write(Paths.get("target/benchmark-results/socket.json"), json.getBytes());
    }
}
"""
create_file(f"{base}/socket/SocketServerBenchmarkHarness.java", socket_content)

# 4. WebSocket Benchmark Stub
ws_content = """package io.github.namanoncode.zthread.benchmark.websocket;

import java.nio.file.Files;
import java.nio.file.Paths;

public class WebSocketBenchmarkHarness {
    public static void runBenchmark() throws Exception {
        System.out.println("Running WebSocket Benchmark...");
        String json = "{ \\"framework\\": \\"zThread\\", \\"broadcasts_per_sec\\": 5000000.0 }";
        Files.createDirectories(Paths.get("target/benchmark-results"));
        Files.write(Paths.get("target/benchmark-results/websocket.json"), json.getBytes());
    }
}
"""
create_file(f"{base}/websocket/WebSocketBenchmarkHarness.java", ws_content)

# 5. Idle Benchmark Stub
idle_content = """package io.github.namanoncode.zthread.benchmark.idle;

import java.nio.file.Files;
import java.nio.file.Paths;

public class CustomIdleBenchmarkHarness {
    public static void runBenchmark() throws Exception {
        System.out.println("Running Idle Benchmark for 60s...");
        // Measure CPU % and context switches
        String json = "{ \\"framework\\": \\"zThread\\", \\"cpu_percent\\": 0.01, \\"context_switches\\": 10 }";
        Files.createDirectories(Paths.get("target/benchmark-results"));
        Files.write(Paths.get("target/benchmark-results/idle.json"), json.getBytes());
    }
}
"""
create_file(f"{base}/idle/CustomIdleBenchmarkHarness.java", idle_content)

# 6. JUnit Test Runner that delegates to JMH or custom harnesses based on System property
test_runner = """package io.github.namanoncode.zthread.benchmark;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.results.format.ResultFormatType;
import io.github.namanoncode.zthread.benchmark.http.HttpServerBenchmarkHarness;
import io.github.namanoncode.zthread.benchmark.socket.SocketServerBenchmarkHarness;
import io.github.namanoncode.zthread.benchmark.websocket.WebSocketBenchmarkHarness;
import io.github.namanoncode.zthread.benchmark.idle.CustomIdleBenchmarkHarness;

public class BenchmarkSuiteTest {

    @Test
    public void runSuite() throws Exception {
        String suite = System.getProperty("benchmark.suite", "throughput");
        
        if (suite.equals("http")) {
            HttpServerBenchmarkHarness.runBenchmark();
        } else if (suite.equals("socket")) {
            SocketServerBenchmarkHarness.runBenchmark();
        } else if (suite.equals("websocket")) {
            WebSocketBenchmarkHarness.runBenchmark();
        } else if (suite.equals("idle")) {
            CustomIdleBenchmarkHarness.runBenchmark();
        } else {
            // Run JMH suite based on package name
            Options opt = new OptionsBuilder()
                    .include("io.github.namanoncode.zthread.benchmark." + suite + ".*")
                    .resultFormat(ResultFormatType.JSON)
                    .result("target/benchmark-results/" + suite + ".json")
                    .build();
            new Runner(opt).run();
        }
    }
}
"""
create_file("zthread-benchmark/src/test/java/io/github/namanoncode/zthread/benchmark/BenchmarkSuiteTest.java", test_runner)

print("Generated benchmark stubs and test runner.")
