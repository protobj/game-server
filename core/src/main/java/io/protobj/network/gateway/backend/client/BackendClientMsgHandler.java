package io.protobj.network.gateway.backend.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.protobj.network.Command;
import io.protobj.network.MsgDispatcher;
import io.protobj.network.Serilizer;
import io.protobj.network.gateway.backend.client.session.Session;
import io.protobj.network.gateway.backend.client.session.SessionCache;

@ChannelHandler.Sharable
public class BackendClientMsgHandler extends ChannelInboundHandlerAdapter {
    private SessionCache sessionCache;

    private Serilizer serilizer;

    private MsgDispatcher msgDispatcher;

    public BackendClientMsgHandler(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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
        //submit evt
    }

    private void forward(ByteBuf buf, Channel channel) {
        int channelId = buf.readInt();
        Session session = sessionCache.getSessionById(channelId);
        Object msg = serilizer.toObject(buf);
        msgDispatcher.dispatch(session, msg);
    }

}
