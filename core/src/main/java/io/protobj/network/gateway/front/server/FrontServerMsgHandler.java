package io.protobj.network.gateway.front.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.gateway.backend.BackendCommand;
import io.protobj.network.gateway.backend.server.BackendServerCache;
import io.protobj.network.gateway.backend.server.BackendServerSession;
import io.protobj.network.gateway.front.FrontCommand;
import io.protobj.network.gateway.ErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

import static io.protobj.network.gateway.IGatewayServer.FRONT_SESSION;


@ChannelHandler.Sharable
public class FrontServerMsgHandler extends ChannelInboundHandlerAdapter {


    private final NettyGateServer nettyGateServer;

    public FrontServerMsgHandler(NettyGateServer nettyGateServer) {
        this.nettyGateServer = nettyGateServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        byte cmd = buf.readByte();
        Channel channel = ctx.channel();
        if (cmd == FrontCommand.Heartbeat.getCommand()) {
            heartbeat(ctx, cmd);
        } else if (cmd == FrontCommand.Forward.getCommand()) {
            forward(ctx, buf, channel);
        } else if (cmd == FrontCommand.Close.getCommand()) {
            close(ctx);
        } else {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.ERR_COMMAND));
        }

    }

    private void close(ChannelHandlerContext ctx) {
        FrontServerSession frontServerSession = ctx.channel().attr(FRONT_SESSION).get();
        BackendServerCache backendServerCache = nettyGateServer.getBackendCache();
        List<BackendServerSession> serverSession = backendServerCache.getServerSession(frontServerSession.getSid());
        if (CollectionUtils.isEmpty(serverSession)) {
            return;
        }
        ByteBuf buffer = ctx.alloc().buffer(7);
        buffer.writeShort(5);
        buffer.writeByte(BackendCommand.Forward.getCommand());//转发消息
        buffer.writeInt(frontServerSession.getId());
        Channel serverChannel = serverSession.get(RandomUtils.nextInt(0, serverSession.size())).getChannel();
        serverChannel.writeAndFlush(buffer);
        frontServerSession.getChannel().close();
        FrontServerCache frontServerCache = nettyGateServer.getFrontCache();
        frontServerCache.remove(frontServerSession);
    }

    private void heartbeat(ChannelHandlerContext ctx, byte cmd) {
        ByteBuf buf;
        buf = ctx.channel().alloc().buffer(3);
        buf.writeShort(1);
        buf.writeByte(cmd);
        ctx.channel().writeAndFlush(buf);
    }

    private void forward(ChannelHandlerContext ctx, ByteBuf buf, Channel channel) {
        FrontServerSession frontServerSession = channel.attr(FRONT_SESSION).get();
        if (frontServerSession == null) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.NOT_AUTH));
            return;
        }
        BackendServerCache backendServerCache = nettyGateServer.getBackendCache();
        List<BackendServerSession> serverSession = backendServerCache.getServerSession(frontServerSession.getSid());
        if (CollectionUtils.isEmpty(serverSession)) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.SERVER_NOT_ONLINE));
            return;
        }
        ByteBuf buffer = ctx.alloc().buffer(7);
        buffer.writeShort(5 + buf.readableBytes());
        buffer.writeByte(BackendCommand.Forward.getCommand());//转发消息
        buffer.writeInt(frontServerSession.getId());
        Channel serverChannel = serverSession.get(RandomUtils.nextInt(0, serverSession.size())).getChannel();
        serverChannel.write(buffer);
        serverChannel.writeAndFlush(buf);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            switch (idleStateEvent.state()) {
                case READER_IDLE, WRITER_IDLE, ALL_IDLE -> {
                    close(ctx);
                }
                default -> {
                }
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        close(ctx);
        super.channelInactive(ctx);
    }
}
