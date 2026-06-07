package com.paymentswitch.acquirer;

import com.paymentswitch.acquirer.handler.AcquirerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty TCP server that listens for inbound connections from acquirer hosts.
 *
 * <p>Architecture:
 * <pre>
 *   Acquirer Host ──TCP──► AcquirerServer
 *                           │
 *                           ├─ IsoFrameDecoder     (length-field frame splitter)
 *                           ├─ IsoMessageDecoder   (bytes → SwitchMessage)
 *                           ├─ AcquirerHandler     (business logic, triggers routing)
 *                           ├─ IsoMessageEncoder   (SwitchMessage → bytes)
 *                           └─ IsoFrameEncoder     (prepend length header)
 * </pre>
 *
 * <p>Use {@link #start()} / {@link #stop()} for lifecycle management.</p>
 */
public class AcquirerServer {

    private static final Logger log = LoggerFactory.getLogger(AcquirerServer.class);

    private final int port;
    private final AcquirerChannelInitializer channelInitializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;

    public AcquirerServer(int port, AcquirerChannelInitializer channelInitializer) {
        this.port = port;
        this.channelInitializer = channelInitializer;
    }

    /**
     * Bind and start accepting connections. Blocks until the server is bound.
     *
     * @throws InterruptedException if interrupted while binding
     */
    public void start() throws InterruptedException {
        bossGroup   = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();  // defaults to 2 * CPU cores

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        serverChannel = bootstrap.bind(port).sync();
        log.info("AcquirerServer listening on port {}", port);
    }

    /**
     * Gracefully shutdown the server and release all resources.
     */
    public void stop() {
        log.info("Shutting down AcquirerServer...");
        if (serverChannel != null) {
            serverChannel.channel().close();
        }
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup   != null) bossGroup.shutdownGracefully();
        log.info("AcquirerServer stopped.");
    }
}
