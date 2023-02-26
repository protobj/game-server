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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.protobj.network.gateway.external.GateExternalAuthHandler;
import io.protobj.network.gateway.external.GateExternalCache;
import io.protobj.network.gateway.external.GateExternalMsgHandler;
import io.protobj.network.gateway.internal.GateInternalAuthHandler;
import io.protobj.network.gateway.internal.GateInternalCache;
import io.protobj.network.gateway.internal.GateInternalMsgHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NettyGateServer implements IGatewayServer {

    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;

    private final GateExternalAuthHandler gateExternalAuthHandler;
    private final GateExternalMsgHandler gateExternalMsgHandler;


    private final GateExternalCache gateExternalCache = new GateExternalCache();

    private final GateInternalAuthHandler gateInternalAuthHandler;
    private final GateInternalMsgHandler gateInternalMsgHandler;

    private final GateInternalCache gateInternalCache = new GateInternalCache();

    public NettyGateServer(int serverSize) {
        bossGroup = new NioEventLoopGroup(serverSize);
        workerGroup = new NioEventLoopGroup();
        gateExternalAuthHandler = new GateExternalAuthHandler(this);
        gateExternalMsgHandler = new GateExternalMsgHandler(this);

        gateInternalAuthHandler = new GateInternalAuthHandler(this);
        gateInternalMsgHandler = new GateInternalMsgHandler(this);
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
                        ch.pipeline().addLast(gateExternalAuthHandler);
                        ch.pipeline().addLast(gateExternalMsgHandler);
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
                        ch.pipeline().addLast(gateInternalAuthHandler);
                        ch.pipeline().addLast(gateInternalMsgHandler);
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

    public GateInternalCache getBackendCache() {
        return gateInternalCache;
    }

    public GateExternalCache getFrontCache() {
        return gateExternalCache;
    }
}
