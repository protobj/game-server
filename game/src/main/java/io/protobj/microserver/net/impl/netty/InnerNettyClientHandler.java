package io.protobj.microserver.net.impl.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.protobj.microserver.net.MQProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ChannelHandler.Sharable
public class InnerNettyClientHandler extends SimpleChannelInboundHandler<MQProtocol> {

    private static final Logger logger = LoggerFactory.getLogger(InnerNettyClientHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
//        if (channel instanceof UkcpChannel) {
//            ((UkcpChannel) channel).conv(10);
//        }
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MQProtocol protocol) throws Exception {
        //不需要收消息
//        if (!protocol.getMsgId().equals(NtceSvrHeartbeat.class.getSimpleName())) {
//            logger.error("收到消息：{}", protocol.getMsgId());
//        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idlEvt = (IdleStateEvent) evt;
            switch (idlEvt.state()) {
                case READER_IDLE:
                case WRITER_IDLE:
                case ALL_IDLE:
                    ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if (cause instanceof IOException && (
                "Connection reset by peer".equals(cause.getMessage())
        )
        ) {
            logger.info("channel close:{}->{}", channel.localAddress(), channel.remoteAddress());
        } else {
            logger.error(channel.remoteAddress().toString(), cause);
        }
        if (channel.isActive()) {
            channel.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        super.channelReadComplete(ctx);
    }
}
