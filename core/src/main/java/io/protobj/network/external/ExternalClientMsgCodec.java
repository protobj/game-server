package io.protobj.network.external;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.protobj.network.Serilizer;
import io.protobj.network.internal.message.RqstMessage;
import io.protobj.util.ByteBufUtil;

import java.util.List;
@ChannelHandler.Sharable
public class ExternalClientMsgCodec extends ByteToMessageCodec<RqstMessage> {
    private final Serilizer serilizer;

    public ExternalClientMsgCodec(Serilizer serilizer) {
        this.serilizer = serilizer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RqstMessage msg, ByteBuf out) throws Exception {
        int index = msg.index();
        int size = ByteBufUtil.writeIntCount(index);
        ByteBuf data = serilizer.toByteArray(msg);
        out.writeInt(size + data.readableBytes());
        ByteBufUtil.writeVarInt(out, index);
        out.writeBytes(data);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int index = ByteBufUtil.readVarInt(in);
        Object msg = serilizer.toObject(in);
        out.add(new RqstMessage(index, msg));
    }
}
