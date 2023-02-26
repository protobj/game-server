package io.protobj.network.gateway.external;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.Command;
import io.protobj.network.gateway.internal.GateInternalCache;
import io.protobj.network.gateway.internal.GateInternalSession;
import io.protobj.network.gateway.ErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

import static io.protobj.network.gateway.IGatewayServer.FRONT_SESSION;


@ChannelHandler.Sharable
public class GateExternalMsgHandler extends ChannelInboundHandlerAdapter {


    private final NettyGateServer nettyGateServer;

    public GateExternalMsgHandler(NettyGateServer nettyGateServer) {
        this.nettyGateServer = nettyGateServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        byte cmd = buf.readByte();
        Channel channel = ctx.channel();
        Command command = Command.valueOf(cmd);
        switch (command) {
            case Heartbeat -> heartbeat(ctx, cmd);
            case Close -> close(ctx);
            case Forward -> forward(ctx, buf, channel);
            default -> channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.ERR_COMMAND));
        }
    }

    private void close(ChannelHandlerContext ctx) {
        GateExternalSession gateExternalSession = ctx.channel().attr(FRONT_SESSION).get();
        GateInternalCache gateInternalCache = nettyGateServer.getBackendCache();
        List<GateInternalSession> serverSession = gateInternalCache.getServerSession(gateExternalSession.getSid());
        if (CollectionUtils.isEmpty(serverSession)) {
            return;
        }
        ByteBuf buffer = ctx.alloc().buffer(7);
        buffer.writeShort(5);
        buffer.writeByte(Command.Close.getCommand());//转发消息
        buffer.writeInt(gateExternalSession.getId());
        Channel serverChannel = serverSession.get(RandomUtils.nextInt(0, serverSession.size())).getChannel();
        serverChannel.writeAndFlush(buffer);
        gateExternalSession.getChannel().close();
        GateExternalCache gateExternalCache = nettyGateServer.getFrontCache();
        gateExternalCache.remove(gateExternalSession);
    }

    private void heartbeat(ChannelHandlerContext ctx, byte cmd) {
        ByteBuf buf;
        buf = ctx.channel().alloc().buffer(3);
        buf.writeShort(1);
        buf.writeByte(cmd);
        ctx.channel().writeAndFlush(buf);
    }

    private void forward(ChannelHandlerContext ctx, ByteBuf buf, Channel channel) {
        GateExternalSession gateExternalSession = channel.attr(FRONT_SESSION).get();
        if (gateExternalSession == null) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.NOT_AUTH));
            return;
        }
        GateInternalCache gateInternalCache = nettyGateServer.getBackendCache();
        List<GateInternalSession> serverSession = gateInternalCache.getServerSession(gateExternalSession.getSid());
        if (CollectionUtils.isEmpty(serverSession)) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.SERVER_NOT_ONLINE));
            return;
        }
        ByteBuf buffer = ctx.alloc().buffer(7);
        buffer.writeShort(5 + buf.readableBytes());
        buffer.writeByte(Command.Forward.getCommand());//转发消息
        buffer.writeInt(gateExternalSession.getId());
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
