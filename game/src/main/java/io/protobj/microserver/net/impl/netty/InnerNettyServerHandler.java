package io.protobj.microserver.net.impl.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.protobj.microserver.net.MQProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class InnerNettyServerHandler extends SimpleChannelInboundHandler<MQProtocol> {

    private static final Logger logger = LoggerFactory.getLogger(InnerNettyServerHandler.class);
    private static final AttributeKey<RegServerInfo> ATTRIBUTE_KEY_SVR = AttributeKey.newInstance("ATTR_REG_SERVER_INFO");
    InnerNettyConnector innerNettyTcpConnector;

    public InnerNettyServerHandler(InnerNettyConnector innerNettyTcpConnector) {
        this.innerNettyTcpConnector = innerNettyTcpConnector;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
//        if (channel instanceof UkcpChannel) {
//            ((UkcpChannel) channel).conv(10);
//        }
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MQProtocol mqProtocol) throws Exception {
        Attribute<RegServerInfo> attr = channelHandlerContext.channel().attr(ATTRIBUTE_KEY_SVR);
        RegServerInfo s = attr.get();
        if (s == null) {
            RegServerInfo t = new RegServerInfo();
            attr.set(t);
//TODO            if (mqProtocol.getMsgId().equals(NtceSvrRegister.class.getSimpleName())) {
//                NtceSvrRegister decode = (NtceSvrRegister) innerNettyTcpConnector.getMqSerilizer().decode(mqProtocol.getMsgId(), mqProtocol.getMsgData().toByteArray());
//                t.setReg(decode.getReg());
//                t.setSvr(decode.getSvr());
//            } else {
//                channelHandlerContext.close();
//            }
            return;
        }
        //处理心跳
//        if (mqProtocol.getMsgId().equals(NtceSvrHeartbeat.class.getSimpleName())) {
//            channelHandlerContext.writeAndFlush(mqProtocol);
//            return;
//        }
        NettyConsumer mqProtocolMQContext = innerNettyTcpConnector.bindConsumers.get(s.getSvr());
        if (mqProtocolMQContext == null) {
            logger.info("服务不在线或未绑定 消息收发器 {}", s);
            return;
        }
        mqProtocolMQContext.recv(s.getReg(), mqProtocol);
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
        Attribute<RegServerInfo> attr = channel.attr(ATTRIBUTE_KEY_SVR);
        if (attr != null) {
            RegServerInfo regServerInfo = attr.get();
            String reg = regServerInfo.getReg();
            logger.error(reg, cause);
        } else {
            logger.error(channel.remoteAddress().toString(), cause);
        }
        if (channel.isActive()) {
            channel.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}
