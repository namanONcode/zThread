package io.github.namanoncode.zthread.benchmark;

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
