package io.protobj.network.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.protobj.network.internal.message.RqstMessage;
import io.protobj.network.internal.message.RqstSessionClose;
import io.protobj.msgdispatcher.MsgDispatcher;
import io.protobj.network.Command;
import io.protobj.network.Serilizer;
import io.protobj.network.internal.session.Session;
import io.protobj.network.internal.session.SessionCache;
import io.protobj.util.ByteBufUtil;

@ChannelHandler.Sharable
public class InternalClientMsgHandler extends ChannelInboundHandlerAdapter {
    private final SessionCache sessionCache;

    private final Serilizer serilizer;

    private final MsgDispatcher msgDispatcher;

    public InternalClientMsgHandler(SessionCache sessionCache, Serilizer serilizer, MsgDispatcher msgDispatcher) {
        this.sessionCache = sessionCache;
        this.msgDispatcher = msgDispatcher;
        this.serilizer = serilizer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;

        byte cmd = buf.readByte();
        Command command = Command.valueOf(cmd);
        switch (command) {
            case Forward -> forward(buf, ctx.channel());
            case Close -> close(buf, ctx.channel());
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
        Object msg = serilizer.toObject(buf);
        msgDispatcher.dispatch(session, new RqstMessage(index, msg));
    }

}
