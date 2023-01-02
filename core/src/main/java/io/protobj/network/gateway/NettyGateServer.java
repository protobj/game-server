package io.protobj.network.gateway;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.protobj.network.gateway.backend.server.BackendServerAuthHandler;
import io.protobj.network.gateway.backend.server.BackendServerCache;
import io.protobj.network.gateway.backend.server.BackendServerMsgHandler;
import io.protobj.network.gateway.front.server.FrontServerAuthHandler;
import io.protobj.network.gateway.front.server.FrontServerCache;
import io.protobj.network.gateway.front.server.FrontServerMsgHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NettyGateServer implements IGatewayServer {

    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;

    private final FrontServerAuthHandler frontServerAuthHandler;
    private final FrontServerMsgHandler frontServerMsgHandler;


    private final FrontServerCache frontServerCache = new FrontServerCache();

    private final BackendServerAuthHandler backendServerAuthHandler;
    private final BackendServerMsgHandler backendServerMsgHandler;

    private final BackendServerCache backendServerCache = new BackendServerCache();

    public NettyGateServer(int serverSize) {
        bossGroup = new NioEventLoopGroup(serverSize);
        workerGroup = new NioEventLoopGroup();
        frontServerAuthHandler = new FrontServerAuthHandler(this);
        frontServerMsgHandler = new FrontServerMsgHandler(this);

        backendServerAuthHandler = new BackendServerAuthHandler(this);
        backendServerMsgHandler = new BackendServerMsgHandler(this);
    }

    @Override
    public CompletableFuture<Void> startTcpFrontServer(String host, int... ports) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)//
                .childOption(ChannelOption.SO_REUSEADDR, true)//
                .childOption(ChannelOption.TCP_NODELAY, true)//
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))//
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)//
                .handler(new LoggingHandler(LogLevel.INFO))//`
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(4, 4, 4));
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, 0, 2, 0, 2));
                        ch.pipeline().addLast(frontServerAuthHandler);
                        ch.pipeline().addLast(frontServerMsgHandler);
                    }
                });

        List<CompletableFuture<Void>> of = List.of();
        for (int port : ports) {
            CompletableFuture<Void> voidCompletableFuture = new CompletableFuture<>();
            serverBootstrap.bind(host, port).addListener((ChannelFutureListener) channelFuture -> {
                boolean success = channelFuture.isSuccess();
                if (success) {
                    voidCompletableFuture.complete(null);
                } else {
                    voidCompletableFuture.completeExceptionally(channelFuture.cause());
                }
            });
        }
        return CompletableFuture.allOf(of.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Void> startKcpFrontServer(String host, int... ports) {
        return null;
    }

    @Override
    public CompletableFuture<Void> startWebsocketFrontServer(String host, int... ports) {
        /*ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)//
                .childOption(ChannelOption.SO_REUSEADDR, true)//
                .childOption(ChannelOption.TCP_NODELAY, true)//
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))//
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)//
                .handler(new LoggingHandler(LogLevel.INFO))//`
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec(8 * 1024, 16 * 1024, 16 * 1024));
                        // 聚合解码 HttpRequest/HttpContent/LastHttpContent 到 FullHttpRequest
                        // 保证接收的 Http 请求的完整性
                        ch.pipeline().addLast(new HttpObjectAggregator(16 * 1024 * 1024));
                        // 处理其他的 WebSocketFrame
                        ch.pipeline().addLast(new WebSocketServerProtocolHandler("/websocket"));
                        // 写文件内容，支持异步发送大的码流，一般用于发送文件流
                        ch.pipeline().addLast(new ChunkedWriteHandler());
                        ch.pipeline().addLast(new IdleStateHandler(4, 4, 4));
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2,0,2));
                        ch.pipeline().addLast(backendServerAuthHandler);
                        ch.pipeline().addLast(backendServerMsgHandler);
                    }
                });

        List<CompletableFuture<Void>> of = List.of();
        for (int port : ports) {
            CompletableFuture<Void> voidCompletableFuture = new CompletableFuture<>();
            serverBootstrap.bind(host, port).addListener((ChannelFutureListener) channelFuture -> {
                boolean success = channelFuture.isSuccess();
                if (success) {
                    voidCompletableFuture.complete(null);
                } else {
                    voidCompletableFuture.completeExceptionally(channelFuture.cause());
                }
            });
        }
        return CompletableFuture.allOf(of.toArray(new CompletableFuture[0]));*/

        return null;
    }

    @Override
    public CompletableFuture<Void> startTcpBackendServer(String host, int... ports) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)//
                .childOption(ChannelOption.SO_REUSEADDR, true)//
                .childOption(ChannelOption.TCP_NODELAY, true)//
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))//
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)//
                .handler(new LoggingHandler(LogLevel.INFO))//`
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(4, 4, 4));
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2, 0, 2));
                        ch.pipeline().addLast(backendServerAuthHandler);
                        ch.pipeline().addLast(backendServerMsgHandler);
                    }
                });

        List<CompletableFuture<Void>> of = List.of();
        for (int port : ports) {
            CompletableFuture<Void> voidCompletableFuture = new CompletableFuture<>();
            serverBootstrap.bind(host, port).addListener((ChannelFutureListener) channelFuture -> {
                boolean success = channelFuture.isSuccess();
                if (success) {
                    voidCompletableFuture.complete(null);
                } else {
                    voidCompletableFuture.completeExceptionally(channelFuture.cause());
                }
            });
        }
        return CompletableFuture.allOf(of.toArray(new CompletableFuture[0]));
    }

    public BackendServerCache getBackendCache() {
        return backendServerCache;
    }

    public FrontServerCache getFrontCache() {
        return frontServerCache;
    }
}
