package io.protobj.network.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.protobj.msgdispatcher.MsgDispatcher;
import io.protobj.network.Serializer;
import io.protobj.network.internal.session.SessionCache;

import java.util.concurrent.CompletableFuture;

public class NettyInternalClient implements InternalClient {

    private final NioEventLoopGroup workerGroup;

    private final InternalClientAuthHandler internalClientAuthHandler;
    private final InternalClientMsgHandler internalClientMsgHandler;
    //网络消息分发
    public NettyInternalClient(int clientSize, SessionCache sessionCache, MsgDispatcher msgDispatcher, Serializer serializer) {
        this.workerGroup = new NioEventLoopGroup(clientSize);
        internalClientAuthHandler = new InternalClientAuthHandler();
        internalClientMsgHandler = new InternalClientMsgHandler(sessionCache, serializer, msgDispatcher);
    }

    @Override
    public CompletableFuture<Channel> start(String host, int port, int sid) {
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
                        ch.pipeline().addLast(internalClientAuthHandler);
                        ch.pipeline().addLast(internalClientMsgHandler);
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
