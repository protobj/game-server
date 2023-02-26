package io.protobj.network.external;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.protobj.network.Serilizer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

public class NettyExternalClient implements ExternalClient {

    private final EventLoopGroup workerGroup;
    private final ExternalClientAuthHandler externalClientAuthHandler;
    private final ExternalClientMsgHandler externalClientMsgHandler;
    private final ExternalClientMsgCodec externalClientMsgCodec;

    public NettyExternalClient(int size, ThreadFactory factory, Serilizer serilizer) {
        this.workerGroup = new NioEventLoopGroup(size, factory);
        this.externalClientAuthHandler = new ExternalClientAuthHandler();
        this.externalClientMsgCodec = new ExternalClientMsgCodec(serilizer);
        this.externalClientMsgHandler = new ExternalClientMsgHandler();
    }

    @Override
    public CompletableFuture<Channel> connectTcp(String host, int port) {

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        CompletableFuture<Channel> connectFuture = new CompletableFuture<>();
        bootstrap.channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)//
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))//
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)//
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(4, 4, 4));
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        ch.pipeline().addLast(externalClientAuthHandler);

                        ch.pipeline().addLast(externalClientMsgCodec);
                        ch.pipeline().addLast(externalClientMsgHandler);
                        ch.attr(CONNECT_FUTURE).set(connectFuture);
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            Throwable cause = future.cause();
            if (cause != null) {
                connectFuture.completeExceptionally(cause);
            }
        });
        return connectFuture;
    }

    @Override
    public CompletableFuture<Channel> connectKcp(String host, int port) {
        return null;
    }

    @Override
    public CompletableFuture<Channel> connectWebsocket(String host, int port) {
        return null;
    }
}
