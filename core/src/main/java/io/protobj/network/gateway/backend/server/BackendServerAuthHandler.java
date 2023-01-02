package io.protobj.network.gateway.backend.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.protobj.network.Command;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.gateway.ErrorCode;

import static io.protobj.network.gateway.IGatewayServer.*;

@ChannelHandler.Sharable
public class BackendServerAuthHandler extends ChannelInboundHandlerAdapter {


    private final NettyGateServer nettyGateServer;

    public BackendServerAuthHandler(NettyGateServer nettyGateServer) {
        this.nettyGateServer = nettyGateServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte b = buf.readByte();
        Channel channel = ctx.channel();
        if (b != Command.Handshake.getCommand()) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.NOT_AUTH));
            return;
        }
        System.err.println("BackendServerAuthHandler " + channel);
        int sid = buf.readInt();

        ByteBuf buffer = channel.alloc().buffer(3);
        buffer.writeShort(1);
        buffer.writeByte(Command.Handshake.getCommand());//连接成功
        channel.writeAndFlush(buffer);
        channel.pipeline().remove(this);
        BackendServerCache backendServerCache = nettyGateServer.getBackendCache();
        BackendServerSession session = backendServerCache.putSession(sid, ctx.channel());
        channel.attr(BACKEND_SESSION).set(session);
    }
}
