package io.protobj.network.external;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.protobj.network.Serializer;
import io.protobj.network.internal.message.RqstMessage;
import io.protobj.util.ByteBufUtil;

import java.util.List;
@ChannelHandler.Sharable
public class ExternalClientMsgCodec extends ByteToMessageCodec<RqstMessage> {
    private final Serializer serializer;

    public ExternalClientMsgCodec(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RqstMessage msg, ByteBuf out) throws Exception {
        int index = msg.index();
        int size = ByteBufUtil.writeIntCount(index);
        ByteBuf data = serializer.toByteArray(msg);
        out.writeInt(size + data.readableBytes());
        ByteBufUtil.writeVarInt(out, index);
        out.writeBytes(data);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int index = ByteBufUtil.readVarInt(in);
        Object msg = serializer.toObject(in);
        out.add(new RqstMessage(index, msg));
    }
}
