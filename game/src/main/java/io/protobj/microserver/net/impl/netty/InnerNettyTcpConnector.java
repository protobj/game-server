package io.protobj.microserver.net.impl.netty;

import com.guangyu.cd003.projects.message.core.net.MQSerilizer;
import com.guangyu.cd003.projects.microserver.log.ThreadLocalLoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

public class InnerNettyTcpConnector extends InnerNettyConnector {

    public InnerNettyTcpConnector(MQSerilizer mqSerilizer) {
        super(mqSerilizer);
    }

    @Override
    public void init(String host, int port) {
        boolean useEpoll = nettyConfig.isUseEpoll();
        try {

            Class<? extends ServerChannel> serverChannelClass = getServerChannelClass(useEpoll);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(serverChannelClass)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(ChannelOption.SO_BACKLOG, nettyConfig.getSoBackLog())
                    .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.isSoKeepAlive())
                    .childOption(ChannelOption.SO_LINGER, nettyConfig.getSoLinger())
                    .childOption(ChannelOption.TCP_NODELAY, nettyConfig.isTcpNoDelay())
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
                    .option(ChannelOption.SO_KEEPALIVE, nettyConfig.isSoKeepAlive())
                    .option(ChannelOption.SO_LINGER, nettyConfig.getSoLinger())
                    .option(ChannelOption.TCP_NODELAY, nettyConfig.isTcpNoDelay())
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
