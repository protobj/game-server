package io.protobj.microserver.net.impl.netty;

import com.guangyu.cd003.projects.message.core.net.MQSerilizer;
import io.jpower.kcp.netty.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.UkcpServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class InnerNettyKcpConnector extends InnerNettyConnector {

    public InnerNettyKcpConnector(MQSerilizer mqSerilizer) {
        super(mqSerilizer);
    }

    @Override
    public void init(String host, int port) {

        UkcpServerBootstrap serverBootstrap = new UkcpServerBootstrap();
        serverBootstrap.group(bossGroup)
                .channel(UkcpServerChannel.class)
                .childHandler(new ChannelInitializer<UkcpChannel>() {
                    @Override
                    public void initChannel(UkcpChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        initPipeline(p);
                        p.addLast(innerNettyServerHandler);
                    }
                });
        ChannelOptionHelper.nodelay(serverBootstrap, true, 20, 2, true)
                .childOption(UkcpChannelOption.UKCP_MTU, 512);

        // Start the server.
        try {
            serverBootstrap.bind(host, port).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Bootstrap b = new Bootstrap();
        b.group(workerGroup)
                .channel(UkcpClientChannel.class)
                .handler(new ChannelInitializer<UkcpChannel>() {
                    @Override
                    public void initChannel(UkcpChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        initPipeline(p);
                        p.addLast(innerNettyClientHandler);
                    }
                });
        ChannelOptionHelper.nodelay(b, true, 20, 2, true)
                .option(UkcpChannelOption.UKCP_MTU, 512);
        this.clientBootstrap = b;
    }
}
