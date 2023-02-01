package io.protobj.network.gateway;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.protobj.msgdispatcher.MsgDispatcher;
import io.protobj.network.gateway.backend.client.BackendClientAuthHandler;
import io.protobj.network.gateway.backend.client.BackendClientMsgHandler;
import io.protobj.network.gateway.backend.client.session.SessionCache;

import java.util.concurrent.CompletableFuture;

public class NettyGateClient implements IGateClient {


    private final NioEventLoopGroup workerGroup;

    private final BackendClientAuthHandler backendClientAuthHandler;
    private final BackendClientMsgHandler backendClientMsgHandler;
    //网络消息分发
    private MsgDispatcher msgDispatcher;

    public NettyGateClient(int clientSize, SessionCache sessionCache) {
        this.workerGroup = new NioEventLoopGroup(clientSize);
        backendClientAuthHandler = new BackendClientAuthHandler();
        backendClientMsgHandler = new BackendClientMsgHandler(sessionCache);
    }

    @Override
    public CompletableFuture<Channel> startTcpFrontClient(String host, int port) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Channel> startTcpBackendClient(String host, int port, int sid) {
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
                        ch.attr(SID).set(sid);
                        ch.pipeline().addLast(new IdleStateHandler(4, 4, 4));
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2, 0, 2));
                        ch.pipeline().addLast(backendClientAuthHandler);
                        ch.pipeline().addLast(backendClientMsgHandler);
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
}
