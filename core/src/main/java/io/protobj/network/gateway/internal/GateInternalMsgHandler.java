package io.protobj.network.gateway.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.Command;
import io.protobj.network.gateway.external.GateExternalCache;
import io.protobj.network.gateway.external.GateExternalSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static io.protobj.network.gateway.IGatewayServer.BACKEND_SESSION;


@ChannelHandler.Sharable
public class GateInternalMsgHandler extends ChannelInboundHandlerAdapter {

    private final NettyGateServer nettyGateServer;

    public GateInternalMsgHandler(NettyGateServer nettyGateServer) {
        this.nettyGateServer = nettyGateServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte cmd = buf.readByte();
        Channel channel = ctx.channel();
         GateInternalSession session = channel.attr(BACKEND_SESSION).get();
        Command command = Command.valueOf(cmd);
        if (command == null) {
            ctx.fireExceptionCaught(new UnsupportedOperationException("error cmd for backend %d".formatted(cmd)));
            return;
        }
        switch (command) {
            case Unicast -> unicast(ctx, buf, session);
            case Multicast -> multicast(ctx, buf, session);
            case Broadcast -> broadcast(ctx, buf, session);
            case Heartbeat -> heartbeat(ctx, cmd);
            case ERR -> err(ctx, buf, channel);
            default ->
                    ctx.fireExceptionCaught(new UnsupportedOperationException("error cmd for backend %s".formatted(command)));
        }
    }

    private static void err(ChannelHandlerContext ctx, ByteBuf buf, Channel channel) {
        byte b = buf.readByte();
        ctx.fireExceptionCaught(new Exception("error code from %s %d".formatted(channel, b)));
    }

    private void heartbeat(ChannelHandlerContext ctx, byte cmd) {
        ByteBuf buffer = ctx.channel().alloc().buffer(3);
        buffer.writeShort(1);
        buffer.writeByte(cmd);
        ctx.channel().writeAndFlush(buffer);
    }

    private void multicast(ChannelHandlerContext ctx, ByteBuf buf, GateInternalSession session) {
        Collection<GateExternalSession> sessions;
        Map<Integer, GateExternalSession> frontSessions = nettyGateServer.getFrontCache().getSessions(session.getSid());
        if (buf.isDirect()) {
            int size = buf.readInt();
            ByteBuf byteBuf = ctx.alloc().buffer(size * 4);
            buf.readBytes(byteBuf);
            sessions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                GateExternalSession gateExternalSession = frontSessions.get(buf.readInt());
                if (gateExternalSession != null) {
                    sessions.add(gateExternalSession);
                }
            }
            byteBuf.release();
        } else {
            int size = buf.readInt();
            sessions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                GateExternalSession gateExternalSession = frontSessions.get(buf.readInt());
                if (gateExternalSession != null) {
                    sessions.add(gateExternalSession);
                }
            }
            multiSend(ctx, buf, sessions);
        }
    }

    private void broadcast(ChannelHandlerContext ctx, ByteBuf buf, GateInternalSession session) {
        int sid = session.getSid();
        Map<Integer, GateExternalSession> sessions = nettyGateServer.getFrontCache().getSessions(sid);
        if (sessions != null) {
            multiSend(ctx, buf, sessions.values());
        }
    }

    private void multiSend(ChannelHandlerContext ctx, ByteBuf buf, Collection<GateExternalSession> sessions) {
        ByteBuf buffer = ctx.alloc().buffer(5);
        buffer.writeInt(1 + buf.readableBytes());
        buffer.writeByte(Command.Forward.getCommand());
        CompositeByteBuf byteBufs = ctx.alloc().compositeBuffer(2);
        byteBufs.addComponent(buffer);
        byteBufs.addComponent(buf);
        if (sessions != null) {
            for (GateExternalSession value : sessions) {
                ByteBuf byteBuf = byteBufs.retainedDuplicate();
                value.getChannel().writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> byteBuf.release());
            }
        }
    }

    private boolean unicast(ChannelHandlerContext ctx, ByteBuf buf, GateInternalSession session) {
        int frontId = buf.readInt();
        int sid = session.getSid();
        GateExternalSession gateExternalSession = nettyGateServer.getFrontCache().getSession(frontId, sid);
        if (gateExternalSession == null) {
            ctx.fireExceptionCaught(new NullPointerException("front session not found %d".formatted(frontId)));
            return true;
        }
        Channel frontSessionChannel = gateExternalSession.getChannel();
        ByteBuf buffer = frontSessionChannel.alloc().buffer(5);
        buffer.writeInt(1 + buf.readableBytes());
        buffer.writeByte(Command.Forward.getCommand());
        frontSessionChannel.write(buffer);
        frontSessionChannel.writeAndFlush(buf);
        return false;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            switch (idleStateEvent.state()) {
                case READER_IDLE, WRITER_IDLE, ALL_IDLE -> {
                    close(ctx, false);
                }
                default -> {
                }
            }
        }
    }

    private void close(ChannelHandlerContext ctx, boolean onClose) {
        GateInternalSession gateInternalSession = ctx.channel().attr(BACKEND_SESSION).get();
        if (gateInternalSession != null) {
            int sid = gateInternalSession.getSid();
            GateExternalCache gateExternalCache = nettyGateServer.getFrontCache();
            Map<Integer, GateExternalSession> sessions = gateExternalCache.removeSessions(sid);
            ByteBuf buffer = ctx.alloc().buffer(5);
            buffer.writeInt(1);
            buffer.writeByte(Command.Close.getCommand());
            if (sessions != null) {
                for (GateExternalSession value : sessions.values()) {
                    ChannelFuture channelFuture = value.getChannel().writeAndFlush(buffer.retain());
                    if (!onClose) {
                        channelFuture.addListener((ChannelFutureListener) future -> {
                            value.getChannel().close();
                        });
                    }
                }
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        close(ctx, true);
        super.channelInactive(ctx);
    }
}
