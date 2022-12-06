package io.protobj.network.gateway.backend.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.gateway.backend.BackendCommand;
import io.protobj.network.gateway.front.FrontCommand;
import io.protobj.network.gateway.front.FrontErrorCode;

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
        if (b != BackendCommand.Handshake.getCommand()) {
            channel.writeAndFlush(FrontErrorCode.createErrorMsg(channel, FrontErrorCode.NOT_AUTH));
            return;
        }
        int sid = buf.readInt();

        ByteBuf buffer = channel.alloc().buffer(5);
        buffer.writeShort(1);
        buffer.writeByte(FrontCommand.Handshake.getCommand());//连接成功
        channel.writeAndFlush(buf);
        channel.pipeline().remove(this);
        BackendServerCache backendServerCache = nettyGateServer.getBackendCache();
        BackendServerSession session = backendServerCache.putSession(sid, ctx.channel());
        channel.attr(BACKEND_SESSION).set(session);
    }
}
