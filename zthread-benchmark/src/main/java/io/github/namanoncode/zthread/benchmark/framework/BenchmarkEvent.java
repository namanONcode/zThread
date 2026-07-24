package io.github.namanoncode.zthread.benchmark.framework;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable event used strictly across all benchmark frameworks to ensure
 * fair comparison and identical payload processing.
 */
public final class BenchmarkEvent {
    
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final long id;
    private final long timestampNanos;
    private final byte[] payload;
    private final long checksum;

    public BenchmarkEvent(int payloadSizeBytes) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.timestampNanos = System.nanoTime();
        this.payload = new byte[payloadSizeBytes];
        
        // Fill payload and compute checksum to ensure no JIT dead-code elimination
        long sum = 0;
        for (int i = 0; i < payloadSizeBytes; i++) {
            byte val = (byte) (i % 255);
            this.payload[i] = val;
            sum += val;
        }
        this.checksum = sum ^ this.id;
    }

    public long getId() {
        return id;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getChecksum() {
        return checksum;
    }

    /**
     * Validates the checksum to simulate identical work in handlers across all frameworks.
     */
    public boolean validate() {
        long sum = 0;
        for (byte b : payload) {
            sum += b;
        }
        return checksum == (sum ^ id);
    }
}
