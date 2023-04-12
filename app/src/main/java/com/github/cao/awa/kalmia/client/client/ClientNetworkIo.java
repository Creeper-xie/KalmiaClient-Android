package com.github.cao.awa.kalmia.client.client;

import com.github.cao.awa.apricot.thread.pool.ExecutorFactor;
import com.github.cao.awa.kalmia.network.io.channel.KalmiaClientChannelInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class ClientNetworkIo {
//    private static final Logger LOGGER = LogManager.getLogger("ApricotNetworkIo");
    private static final Supplier<NioEventLoopGroup> DEFAULT_CHANNEL = () -> new NioEventLoopGroup(
            0,
            ExecutorFactor.intensiveCpu()
    );
    private static final Supplier<EpollEventLoopGroup> EPOLL_CHANNEL = () -> new EpollEventLoopGroup(
            0,
            ExecutorFactor.intensiveCpu()
    );

    private final KalmiaClientChannelInitializer channelInitializer;
    private final List<ChannelFuture> channels = new CopyOnWriteArrayList<>();

    public ClientNetworkIo() {
        this.channelInitializer = new KalmiaClientChannelInitializer();
    }

    public void connect(final String ip, final int port) throws Exception {
//        boolean expectEpoll = true;
        boolean epoll = Epoll.isAvailable();

//        LOGGER.info(expectEpoll ?
//                            epoll ?
//                                    "Apricot network io using Epoll" :
//                                    "Apricot network io expected Epoll, but Epoll is not available, switch to NIO" :
//                            "Apricot network io using NIO");

        Supplier<? extends EventLoopGroup> lazy = epoll ? EPOLL_CHANNEL : DEFAULT_CHANNEL;

        Class<? extends SocketChannel> channel = epoll ?
                EpollSocketChannel.class :
                NioSocketChannel.class;

        EventLoopGroup boss = lazy.get();
        EventLoopGroup worker = lazy.get();
        Bootstrap bootstrap = new Bootstrap();
        try {
            this.channels.add(bootstrap.channel(channel)
                                       .group(
                                               boss
                                       )
                                       .option(
                                               // Real-time response is necessary
                                               // Enable TCP no delay to improve response speeds
                                               ChannelOption.TCP_NODELAY,
                                               true
                                       )
                                       .handler(this.channelInitializer)
                                       .connect(
                                               ip,
                                               port
                                       )
                                       .syncUninterruptibly()
                                       .channel()
                                       .closeFuture()
                                       .sync());
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public void shutdown() {
        for (ChannelFuture channelFuture : this.channels) {
            try {
                channelFuture.channel()
                             .close()
                             .sync();
            } catch (InterruptedException interruptedException) {
//                LOGGER.error("Interrupted whilst closing channel");
            }
        }
    }
}