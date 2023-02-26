package io.protobj.network.gateway.internal;

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
public class GateInternalAuthHandler extends ChannelInboundHandlerAdapter {


    private final NettyGateServer nettyGateServer;

    public GateInternalAuthHandler(NettyGateServer nettyGateServer) {
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
        int sid = buf.readInt();

        ByteBuf buffer = channel.alloc().buffer(3);
        buffer.writeShort(1);
        buffer.writeByte(Command.Handshake.getCommand());//连接成功
        channel.writeAndFlush(buffer);
        channel.pipeline().remove(this);
        GateInternalCache gateInternalCache = nettyGateServer.getBackendCache();
        GateInternalSession session = gateInternalCache.putSession(sid, ctx.channel());
        channel.attr(BACKEND_SESSION).set(session);
    }
}
