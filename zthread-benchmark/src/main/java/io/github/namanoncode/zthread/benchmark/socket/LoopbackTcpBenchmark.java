package io.github.namanoncode.zthread.benchmark.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC"})
public class LoopbackTcpBenchmark {

    public static final int BATCH_SIZE = 10_000;
    private static final byte[] PAYLOAD = "bench".getBytes();
    
    // Netty
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Channel clientChannel;

    // Standard NIO (Virtual Threads)
    private java.nio.channels.ServerSocketChannel nioServer;
    private java.nio.channels.SocketChannel nioClient;
    private ExecutorService virtualThreadExecutor;
    private volatile boolean nioRunning;

    private CountDownLatch latch;

    @Setup(Level.Trial)
    public void setup(Blackhole bh) throws Exception {
        setupNetty(bh);
        setupNioVirtualThreads(bh);
        // zThread: TODO implement when LinuxSocketDispatcher is available
    }

    private void setupNetty(Blackhole bh) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                     @Override
                     public void channelRead(ChannelHandlerContext ctx, Object msg) {
                         io.netty.buffer.ByteBuf in = (io.netty.buffer.ByteBuf) msg;
                         bh.consume(in.readableBytes());
                         in.release();
                         if (latch != null) {
                             latch.countDown();
                         }
                     }
                 });
             }
         });

        serverChannel = b.bind(0).sync().channel();
        int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

        Bootstrap cb = new Bootstrap();
        cb.group(workerGroup)
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
              @Override
              public void initChannel(SocketChannel ch) {}
          });

        clientChannel = cb.connect("127.0.0.1", port).sync().channel();
    }

    private void setupNioVirtualThreads(Blackhole bh) throws Exception {
        nioRunning = true;
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        nioServer = java.nio.channels.ServerSocketChannel.open();
        nioServer.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = ((InetSocketAddress) nioServer.getLocalAddress()).getPort();

        virtualThreadExecutor.submit(() -> {
            try {
                java.nio.channels.SocketChannel accepted = nioServer.accept();
                ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                while (nioRunning) {
                    buffer.clear();
                    int read = accepted.read(buffer);
                    if (read > 0) {
                        bh.consume(read);
                        int numMessages = read / PAYLOAD.length;
                        for (int i = 0; i < numMessages; i++) {
                            if (latch != null) {
                                latch.countDown();
                            }
                        }
                    } else if (read == -1) {
                        break;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        });

        nioClient = java.nio.channels.SocketChannel.open(new InetSocketAddress("127.0.0.1", port));
        // Wait for connection to establish and accept thread to start reading
        Thread.sleep(100);
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        clientChannel.close().sync();
        serverChannel.close().sync();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        nioRunning = false;
        nioClient.close();
        nioServer.close();
        virtualThreadExecutor.shutdown();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchNetty(Blackhole bh) throws InterruptedException {
        latch = new CountDownLatch(BATCH_SIZE);
        io.netty.buffer.ByteBuf buf = clientChannel.alloc().buffer(PAYLOAD.length);
        buf.writeBytes(PAYLOAD);
        
        for (int i = 0; i < BATCH_SIZE; i++) {
            clientChannel.write(buf.retainedDuplicate());
        }
        clientChannel.flush();
        buf.release();
        
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void benchNioVirtualThreads(Blackhole bh) throws InterruptedException, IOException {
        latch = new CountDownLatch(BATCH_SIZE);
        ByteBuffer buf = ByteBuffer.wrap(PAYLOAD);
        
        for (int i = 0; i < BATCH_SIZE; i++) {
            buf.position(0);
            while (buf.hasRemaining()) {
                nioClient.write(buf);
            }
        }
        latch.await();
    }
}
