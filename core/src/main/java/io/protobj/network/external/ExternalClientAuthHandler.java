package io.protobj.network.external;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.protobj.network.Command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@ChannelHandler.Sharable
public class ExternalClientAuthHandler extends ChannelInboundHandlerAdapter {


    public static final AttributeKey<AtomicInteger> COUNT = AttributeKey.newInstance("count");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Attribute<AtomicInteger> attr = ctx.attr(COUNT);
        attr.setIfAbsent(new AtomicInteger());
        ByteBuf data = (ByteBuf) msg;

        Command command = Command.valueOf(data.readByte());

        if (command != Command.Handshake) {
            throw new IllegalArgumentException("handshake error");
        }
        AtomicInteger count = attr.get();
        if (count.get() == 0) {
            byte[] token = new byte[data.readableBytes()];
            ByteBuf response = ctx.channel().alloc().buffer(11);
            response.writeInt(7);
            response.writeByte(command.getCommand());
            response.writeBytes(token);
            ctx.channel().writeAndFlush(response);
            count.incrementAndGet();
        } else {
            Attribute<CompletableFuture<Channel>> connectFuture = ctx.attr(ExternalClient.CONNECT_FUTURE);
            connectFuture.get().complete(ctx.channel());
            ctx.pipeline().remove(this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Attribute<CompletableFuture<Channel>> connectFuture = ctx.channel() .attr(ExternalClient.CONNECT_FUTURE);
        connectFuture.get().completeExceptionally(cause);
    }
}

