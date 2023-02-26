package io.protobj.network.internal.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.protobj.network.Command;
import io.protobj.network.Serilizer;
import io.protobj.network.internal.message.*;
import io.protobj.util.ByteBufUtil;

import java.util.List;

public class InternalProtobjCodec extends ByteToMessageCodec<Object> {

    private Serilizer serilizer;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int index = ByteBufUtil.readVarInt(in);
        Object message = serilizer.toObject(in);
        out.add(new RqstMessage(index, message));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof BroadcastMessage broadcastMessage) {
            ByteBuf data = serilizer.toByteArray(broadcastMessage.msg());
            out.writeShort(1 + 1 + data.readableBytes());
            out.writeByte(Command.Broadcast.getCommand());
            out.writeByte(0);//index
            out.writeBytes(data);
        } else if (msg instanceof MulticastMessage multicastMessage) {
            ByteBuf data = serilizer.toByteArray(multicastMessage.msg());
            out.writeShort(1 + 4 + 4 * multicastMessage.channelIds().size() + 1 + data.readableBytes());
            out.writeByte(Command.Multicast.getCommand());
            out.writeInt(multicastMessage.channelIds().size());
            for (Integer id : multicastMessage.channelIds()) {
                out.writeInt(id);
            }
            out.writeByte(0);//index
            out.writeBytes(data);
        } else if (msg instanceof PushMessage pushMessage) {
            ByteBuf data = serilizer.toByteArray(pushMessage.msg());
            out.writeShort(1 + 4 + 1 + data.readableBytes());
            out.writeByte(Command.Unicast.getCommand());
            out.writeInt(pushMessage.channelId());
            out.writeByte(0);
            out.writeBytes(data);
        } else if (msg instanceof UnicastMessage unicastMessage) {
            ByteBuf data = serilizer.toByteArray(unicastMessage.msg());
            out.writeShort(1 + 4 + ByteBufUtil.writeIntCount(unicastMessage.index()) + data.readableBytes());
            out.writeByte(Command.Unicast.getCommand());
            out.writeInt(unicastMessage.channelId());
            ByteBufUtil.writeVarInt(out, unicastMessage.index());
            out.writeBytes(data);

        } else {
            throw new IllegalArgumentException("unknown message type:%s".formatted(msg != null ? msg.getClass().getName() : "null"));
        }
    }

}
