package io.protobj.network.external;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.protobj.network.Command;

@ChannelHandler.Sharable
public class ExternalClientAuthHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf data = (ByteBuf) msg;

        Command command = Command.valueOf(data.readByte());

        if (command != Command.Handshake) {
            throw new IllegalArgumentException("handshake error");
        }
        byte[] token = new byte[data.readableBytes()];
        ByteBuf response = ctx.channel().alloc().buffer(9);
        response.writeShort(7);
        response.writeByte(command.getCommand());
        response.writeBytes(token);
        ctx.channel().writeAndFlush(response);
        ctx.pipeline().remove(this);
    }
}
