package io.protobj.network.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.protobj.msgdispatcher.MsgDispatcher;
import io.protobj.network.Command;
import io.protobj.network.Serializer;
import io.protobj.network.internal.message.*;
import io.protobj.network.internal.session.Session;
import io.protobj.network.internal.session.SessionCache;
import io.protobj.util.ByteBufUtil;

import java.util.List;

@ChannelHandler.Sharable
public class InternalClientMsgHandler extends ByteToMessageCodec<Object> {
    private final SessionCache sessionCache;

    private final Serializer serializer;

    private final MsgDispatcher msgDispatcher;

    public InternalClientMsgHandler(SessionCache sessionCache, Serializer serializer, MsgDispatcher msgDispatcher) {
        this.sessionCache = sessionCache;
        this.msgDispatcher = msgDispatcher;
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof BroadcastMessage broadcastMessage) {
            ByteBuf data = serializer.toByteArray(broadcastMessage.msg());
            out.writeInt(1 + 1 + data.readableBytes());
            out.writeByte(Command.Broadcast.getCommand());
            out.writeByte(0);//index
            out.writeBytes(data);
        } else if (msg instanceof MulticastMessage multicastMessage) {
            ByteBuf data = serializer.toByteArray(multicastMessage.msg());
            out.writeInt(1 + 4 + 4 * multicastMessage.channelIds().size() + 1 + data.readableBytes());
            out.writeByte(Command.Multicast.getCommand());
            out.writeInt(multicastMessage.channelIds().size());
            for (Integer id : multicastMessage.channelIds()) {
                out.writeInt(id);
            }
            out.writeByte(0);//index
            out.writeBytes(data);
        } else if (msg instanceof PushMessage pushMessage) {
            ByteBuf data = serializer.toByteArray(pushMessage.msg());
            out.writeInt(1 + 4 + 1 + data.readableBytes());
            out.writeByte(Command.Unicast.getCommand());
            out.writeInt(pushMessage.channelId());
            out.writeByte(0);
            out.writeBytes(data);
        } else if (msg instanceof UnicastMessage unicastMessage) {
            ByteBuf data = serializer.toByteArray(unicastMessage.msg());
            out.writeInt(1 + 4 + ByteBufUtil.writeIntCount(unicastMessage.index()) + data.readableBytes());
            out.writeByte(Command.Unicast.getCommand());
            out.writeInt(unicastMessage.channelId());
            ByteBufUtil.writeVarInt(out, unicastMessage.index());
            out.writeBytes(data);
        } else {
            throw new IllegalArgumentException("unknown message type:%s".formatted(msg != null ? msg.getClass().getName() : "null"));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte cmd = in.readByte();
        Command command = Command.valueOf(cmd);
        switch (command) {
            case Forward -> forward(in, ctx.channel());
            case Close -> close(in, ctx.channel());
        }
    }

    private void close(ByteBuf buf, Channel channel) {
        int channelId = buf.readInt();
        Session session = sessionCache.getSessionById(channelId);
        msgDispatcher.dispatch(session, new RqstMessage(0, new RqstSessionClose()));
    }

    private void forward(ByteBuf buf, Channel channel) {
        int channelId = buf.readInt();
        int index = ByteBufUtil.readVarInt(buf);
        Session session = sessionCache.getSessionById(channelId);
        Object msg = serializer.toObject(buf);
        msgDispatcher.dispatch(session, new RqstMessage(index, msg));
    }

}
