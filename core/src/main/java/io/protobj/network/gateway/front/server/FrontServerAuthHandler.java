package io.protobj.network.gateway.front.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.gateway.front.FrontCommand;
import io.protobj.network.gateway.ErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Arrays;

import static io.protobj.network.gateway.IGatewayServer.FRONT_SESSION;
import static io.protobj.network.gateway.IGatewayServer.TOKEN;


@ChannelHandler.Sharable
public class FrontServerAuthHandler extends ChannelInboundHandlerAdapter {


    private final NettyGateServer nettyGateServer;

    public FrontServerAuthHandler(NettyGateServer nettyGateServer) {
        this.nettyGateServer = nettyGateServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        byte[] token = RandomStringUtils.randomAlphabetic(6).getBytes();
        channel.attr(TOKEN).set(token);
        ByteBuf buffer = channel.alloc().buffer(9);
        buffer.writeShort(7);
        buffer.writeByte(FrontCommand.Handshake.getCommand());//1
        buffer.writeBytes(token);//6
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte b = buf.readByte();
        Channel channel = ctx.channel();
        if (b != FrontCommand.Handshake.getCommand()) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.NOT_AUTH));
            return;
        }
        byte[] s = channel.attr(TOKEN).get();
        if (s == null) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.UNKNOWN));
            return;
        }
        int sid = buf.readInt();
        byte[] tokenBytes = new byte[6];
        buf.readBytes(tokenBytes);
        if (!Arrays.equals(tokenBytes, s)) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.ERR_TOKEN));
            return;
        }
        if (!CollectionUtils.isNotEmpty(nettyGateServer.getBackendCache().getServerSession(sid))) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.SERVER_NOT_ONLINE));
            return;
        }

        ByteBuf buffer = channel.alloc().buffer(3);
        buffer.writeShort(1);
        buffer.writeByte(FrontCommand.Handshake.getCommand());//连接成功
        channel.writeAndFlush(buf);
        channel.pipeline().remove(this);
        FrontServerCache frontServerCache = nettyGateServer.getFrontCache();
        int id = frontServerCache.generateId();
        FrontServerSession frontServerSession = new FrontServerSession(id, sid, channel);
        frontServerCache.putSession(frontServerSession);
        channel.attr(FRONT_SESSION).set(frontServerSession);
    }
}
