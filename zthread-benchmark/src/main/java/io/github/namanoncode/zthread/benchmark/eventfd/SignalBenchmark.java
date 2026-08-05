package io.github.namanoncode.zthread.benchmark.eventfd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 3)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class SignalBenchmark {

    public static final int BATCH_SIZE = 10_000;
    private static final String SIGNAL_NAME = "USR1";
    
    private Signal signal;
    private SignalHandler oldHandler;

    private CountDownLatch latch;
    private long pid;

    @Setup(Level.Trial)
    public void setup(Blackhole bh) {
        pid = ProcessHandle.current().pid();
        signal = new Signal(SIGNAL_NAME);
        
        // Java standard signal handling
        oldHandler = Signal.handle(signal, sig -> {
            bh.consume(sig.getNumber());
            if (latch != null) {
                latch.countDown();
            }
        });
        
        // zThread: TODO implement when LinuxSignalDispatcher is available
    }

    @TearDown(Level.Trial)
    public void teardown() {
        if (oldHandler != null) {
            Signal.handle(signal, oldHandler);
        }
    }

    private void sendSignal() {
        // Send signal via shell since pure Java doesn't have a POSIX kill API without FFM/JNA
        try {
            new ProcessBuilder("kill", "-SIGUSR1", String.valueOf(pid)).start().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchJavaSignal(Blackhole bh) throws InterruptedException {
        latch = new CountDownLatch(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            sendSignal();
        }
        latch.await();
    }
}
