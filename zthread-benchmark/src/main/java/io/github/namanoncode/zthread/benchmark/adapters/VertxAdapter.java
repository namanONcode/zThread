package io.github.namanoncode.zthread.benchmark.adapters;

import io.github.namanoncode.zthread.benchmark.adapters.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.adapters.EventHandler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;

public class VertxAdapter implements EventRuntimeAdapter {

    private Vertx vertx;
    private EventBus eventBus;
    private static final String ADDRESS = "benchmark.events";

    @Override
    public void start(EventHandler handler, int consumers) {
        VertxOptions options = new VertxOptions()
            .setEventLoopPoolSize(consumers);
        this.vertx = Vertx.vertx(options);
        this.eventBus = vertx.eventBus();

        io.vertx.core.eventbus.MessageConsumer<Object> consumer = eventBus.localConsumer(ADDRESS);
        try {
            consumer.getClass().getMethod("setMaxBufferedMessages", int.class).invoke(consumer, 10_000_000);
        } catch (Exception e) {
            // Ignore if missing in newer Vert.x versions
        }
        consumer.handler(message -> {
            handler.onEvent((BenchmarkEvent) message.body());
        });
    }

    @Override
    public void submit(BenchmarkEvent event) {
        eventBus.send(ADDRESS, event);
    }

    @Override
    public void shutdown() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
