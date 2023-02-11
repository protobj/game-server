package io.protobj.microserver.net.impl.netty;

import com.guangyu.cd003.projects.message.core.net.MQProtocol;
import com.guangyu.cd003.projects.message.core.net.MQSerilizer;
import com.guangyu.cd003.projects.message.core.net.NetNotActiveException;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.microserver.log.ThreadLocalLoggerFactory;
import com.pv.common.net.netty.NettyConfig;
import com.pv.common.utilities.common.CommonUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;

import java.util.Map;

public abstract class InnerNettyConnector {

    private static final Logger logger = ThreadLocalLoggerFactory.getLogger(InnerNettyConnector.class);

    protected EventLoopGroup bossGroup;

    protected EventLoopGroup workerGroup;

    protected MQSerilizer mqSerilizer;

    Map<String, NettyConsumer> bindConsumers = CommonUtil.createMap();

    protected Bootstrap clientBootstrap;
    InnerNettyClientHandler innerNettyClientHandler = new InnerNettyClientHandler();
    InnerNettyServerHandler innerNettyServerHandler = new InnerNettyServerHandler(this);

    NettyConfig nettyConfig = new NettyConfig();

    public InnerNettyConnector(MQSerilizer mqSerilizer) {
        this.mqSerilizer = mqSerilizer;
        DefaultThreadFactory bossThreadFactory = new DefaultThreadFactory(
                String.format(nettyConfig.getBossThreadNamePattern(), "inner-netty-boss"));
        bossGroup = nettyConfig.isUseEpoll() ? new EpollEventLoopGroup(1, bossThreadFactory)
                : new NioEventLoopGroup(1, bossThreadFactory); // (1)

        DefaultThreadFactory workerThreadFactory = new DefaultThreadFactory(
                String.format(nettyConfig.getWorkerThreadNamePattern(), "inner-netty-worker"));
        int workerThreads = nettyConfig.getWorkerThreads();
        workerGroup = nettyConfig.isUseEpoll() ? new EpollEventLoopGroup(workerThreads, workerThreadFactory)
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
        int readIdleTimeout = nettyConfig.getReadIdleTimeout();
        int writeIdleTimeout = nettyConfig.getWriteIdleTimeout();
        int allIdleTimeout = nettyConfig.getReadIdleTimeout();
        int writeTimeout = nettyConfig.getWriteTimeout();
        if (readIdleTimeout > 0 || writeIdleTimeout > 0 || allIdleTimeout > 0) {
            p.addLast("idle", new IdleStateHandler(Math.max(0, readIdleTimeout), Math.max(0, writeIdleTimeout),
                    Math.max(0, allIdleTimeout)));
        }
        if (writeTimeout > 0)
            p.addLast("write", new WriteTimeoutHandler(Math.max(0, writeTimeout)));
        p.addLast(new ProtobufVarint32FrameDecoder());
        p.addLast(new ProtobufDecoder(MQProtocol.getDefaultInstance()));
        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());

    }
}
