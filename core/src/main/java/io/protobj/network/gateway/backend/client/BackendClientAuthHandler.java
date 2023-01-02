package io.protobj.network.gateway.backend.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.protobj.network.Command;
import io.protobj.network.gateway.ErrorCode;
import io.protobj.network.gateway.IGateClient;

import java.util.concurrent.CompletableFuture;

@ChannelHandler.Sharable
public class BackendClientAuthHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();

        Attribute<Integer> attr = channel.attr(IGateClient.SID);
        Integer sid = attr.get();

        ByteBuf buffer = channel.alloc().buffer(7);
        buffer.writeShort(1 + 4);
        buffer.writeByte(Command.Handshake.getCommand());
        buffer.writeInt(sid);
        channel.writeAndFlush(buffer);
        System.err.println("BackendClientAuthHandler channelActive " + channel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        byte b = buf.readByte();
        Channel channel = ctx.channel();
        System.err.println("BackendClientAuthHandler channelRead " + channel);
        if (b != Command.Handshake.getCommand()) {
            channel.writeAndFlush(ErrorCode.createErrorMsg(channel, ErrorCode.NOT_AUTH));
            return;
        }
        channel.pipeline().remove(this);
        CompletableFuture<Channel> future = channel.attr(IGateClient.CONNECT_FUTURE).get();
        future.complete(channel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        CompletableFuture<Channel> future = ctx.channel().attr(IGateClient.CONNECT_FUTURE).get();
        future.completeExceptionally(cause);
    }
}
