package io.protobj.microserver.net.impl.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.protobj.microserver.net.MQSerilizer;
import org.slf4j.Logger;

public class InnerNettyTcpConnector extends InnerNettyConnector {

    public InnerNettyTcpConnector(MQSerilizer mqSerilizer) {
        super(mqSerilizer);
    }

    @Override
    public void init(String host, int port) {
        boolean useEpoll = Epoll.isAvailable();
        try {

            Class<? extends ServerChannel> serverChannelClass = getServerChannelClass(useEpoll);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(serverChannelClass)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_LINGER, 0)
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            initPipeline(p);
                            p.addLast(innerNettyServerHandler);
                        }
                    });
            bootstrap.bind(host, port).get();
            clientBootstrap = new Bootstrap();
            Class<? extends SocketChannel> clientChannelClass = getClientChannelClass(useEpoll);
            clientBootstrap.group(workerGroup)
                    .channel(clientChannelClass)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(ChannelOption.SO_KEEPALIVE,true)
                    .option(ChannelOption.SO_LINGER, 0)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            initPipeline(p);
                            p.addLast(innerNettyClientHandler);
                        }
                    });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    protected Class<? extends SocketChannel> getClientChannelClass(boolean useEpoll) {
        return useEpoll ? EpollSocketChannel.class
                : NioSocketChannel.class;
    }

    protected Class<? extends ServerChannel> getServerChannelClass(boolean useEpoll) {
        return useEpoll ? EpollServerSocketChannel.class
                : NioServerSocketChannel.class;
    }
}
