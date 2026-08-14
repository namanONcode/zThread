package io.github.namanoncode.zthread.benchmark.adapters;

import io.github.namanoncode.zthread.benchmark.adapters.BenchmarkEvent;
import io.github.namanoncode.zthread.benchmark.adapters.EventHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;

public class NettyAdapter implements EventRuntimeAdapter {

    private EventLoopGroup group;
    private EventHandler handler;

    @Override
    public void start(EventHandler handler, int consumers) {
        this.handler = handler;
        // Use Epoll if available on Linux, otherwise fallback to NIO
        try {
            group = new EpollEventLoopGroup(consumers);
        } catch (Throwable t) {
            group = new NioEventLoopGroup(consumers);
        }
    }

    @Override
    public void submit(BenchmarkEvent event) {
        group.execute(() -> handler.onEvent(event));
    }

    @Override
    public void shutdown() {
        if (group != null) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS);
            try {
                group.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
