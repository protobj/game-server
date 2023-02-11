package io.protobj.microserver.net.impl.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.protobj.microserver.net.MQSerilizer;
import io.protobj.microserver.net.NetNotActiveException;
import io.protobj.microserver.serverregistry.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class InnerNettyConnector {

    private static final Logger logger = LoggerFactory.getLogger(InnerNettyConnector.class);

    protected EventLoopGroup bossGroup;

    protected EventLoopGroup workerGroup;

    protected MQSerilizer mqSerilizer;

    Map<String, NettyConsumer> bindConsumers = new ConcurrentHashMap<>();

    protected Bootstrap clientBootstrap;
    InnerNettyClientHandler innerNettyClientHandler = new InnerNettyClientHandler();
    InnerNettyServerHandler innerNettyServerHandler = new InnerNettyServerHandler(this);


    public InnerNettyConnector(MQSerilizer mqSerilizer) {
        this.mqSerilizer = mqSerilizer;
        DefaultThreadFactory bossThreadFactory = new DefaultThreadFactory("inner-netty-boss");
        bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1, bossThreadFactory)
                : new NioEventLoopGroup(1, bossThreadFactory); // (1)

        DefaultThreadFactory workerThreadFactory = new DefaultThreadFactory("inner-netty-worker");
        int workerThreads = Runtime.getRuntime().availableProcessors() - 1;
        workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(workerThreads, workerThreadFactory)
                : new NioEventLoopGroup(workerThreads, workerThreadFactory);

    }

    public abstract void init(String host, int port);

    public void bindConsumer(NettyConsumer nettyConsumer) {
        ServerInfo selfInfo = nettyConsumer.getContext().getSelfInfo();
        String fullSrvId = selfInfo.getFullSvrId();
        bindConsumers.put(fullSrvId, nettyConsumer);
    }

    public void unbindConsumer(NettyConsumer nettyConsumer) {
        ServerInfo selfInfo = nettyConsumer.getContext().getSelfInfo();
        String fullSrvId = selfInfo.getFullSvrId();
        bindConsumers.remove(fullSrvId, nettyConsumer);
    }

    public Channel createClientChannel(ServerInfo serverInfo) {
        ChannelFuture connect = clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort());
        try {
            connect.get();
        } catch (Exception e) {
            logger.error("", e);
        }
        if (!connect.channel().isActive()) {
            throw new NetNotActiveException(serverInfo.getFullSvrId());
        }
        return connect.channel();
    }

    public ChannelFuture createClientChannelAsync(ServerInfo serverInfo) {
        return clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort());
    }

    public MQSerilizer getMqSerilizer() {
        return mqSerilizer;
    }


    protected void initPipeline(ChannelPipeline p) {
        int readIdleTimeout = 3000;
        int writeIdleTimeout = 3000;
        int allIdleTimeout = 3000;
        int writeTimeout = 3000;
        if (readIdleTimeout > 0 || writeIdleTimeout > 0 || allIdleTimeout > 0) {
            p.addLast("idle", new IdleStateHandler(Math.max(0, readIdleTimeout), Math.max(0, writeIdleTimeout),
                    Math.max(0, allIdleTimeout)));
        }
        if (writeTimeout > 0)
            p.addLast("write", new WriteTimeoutHandler(Math.max(0, writeTimeout)));
        p.addLast(new ProtobufVarint32FrameDecoder());
//TODO        p.addLast(new ProtobufDecoder(MQProtocol.getDefaultInstance()));
        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());

    }
}
