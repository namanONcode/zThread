package io.github.namanoncode.zthread.benchmark.framework.adapter;

import io.github.namanoncode.zthread.benchmark.framework.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.framework.EventHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class ReactorAdapter implements EventRuntimeAdapter {

    private Sinks.Many<BenchmarkEvent> sink;
    private Scheduler scheduler;

    @Override
    public void start(EventHandler handler, int consumers) {
        sink = Sinks.many().unicast().onBackpressureBuffer();
        scheduler = Schedulers.newParallel("reactor-consumer", consumers);

        sink.asFlux()
            .publishOn(scheduler)
            .subscribe(handler::onEvent);
    }

    @Override
    public void submit(BenchmarkEvent event) {
        sink.tryEmitNext(event);
    }

    @Override
    public void shutdown() {
        if (sink != null) {
            sink.tryEmitComplete();
        }
        if (scheduler != null) {
            scheduler.dispose();
        }
    }
}
