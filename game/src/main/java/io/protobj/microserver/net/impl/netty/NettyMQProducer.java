package io.protobj.microserver.net.impl.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import io.protobj.microserver.net.MQProducer;
import io.protobj.microserver.net.MQProtocol;
import io.protobj.microserver.net.NetNotActiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NettyMQProducer extends MQProducer<MQProtocol> {
    private static final Logger logger = LoggerFactory.getLogger(NettyMQProducer.class);

    private volatile Channel channel;
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public CompletableFuture<?> sendAsync(MQProtocol msg) {
        CompletableFuture<?> completableFuture = new CompletableFuture<>();
        if (!channel.isActive()) {
            throw new NetNotActiveException(getServerInfo().getFullSvrId());
        }
        send(msg, completableFuture);
        return completableFuture;
    }

    private void send(MQProtocol msg, CompletableFuture<?> completableFuture) {
        Channel producerChannel = this.channel;
        producerChannel.writeAndFlush(msg).addListener(future -> {
            Throwable cause = future.cause();
            if (cause != null) {
                completableFuture.completeExceptionally(cause);
            } else {
                completableFuture.complete(null);
                getContext().getMqMsgPrinter().sendLog(getContext().getSelfInfo().getFullSvrId(),
                        getServerInfo().getFullSvrId(), msg.getMsgId(), msg.getAsk());
            }
        });
    }

    @Override
    public synchronized void close() {
        try {
            if (channel != null) {
                channel.eventLoop().execute(() -> {
                    channel.close();
                    if (scheduledFuture != null) {
                        scheduledFuture.cancel(true);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Override
    public void listenDestroy() {
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                NettyMQProducer.this.close();
                getContext().removeProducerById(getServerInfo());
                getContext().removeProducerByType(getServerInfo());
                super.channelInactive(ctx);
            }
        });
    }

    @Override
    public boolean isClose() {
        return !channel.isActive();
    }

    public synchronized void setChannel(Channel channel) {
        this.channel = channel;
        MQProtocol heartbeat = createHeartbeat();
        ScheduledFuture<?> scheduledFuture = this.channel.eventLoop().scheduleAtFixedRate(() -> {
            channel.writeAndFlush(heartbeat);
        }, 0, 1, TimeUnit.SECONDS);
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
            this.scheduledFuture = scheduledFuture;
        }
    }

    private MQProtocol createHeartbeat() {
//        NtceSvrHeartbeat ntceSvrHeartbeat = new NtceSvrHeartbeat();
//        byte[] encode = getContext().getSerilizer().encode(ntceSvrHeartbeat);
//        return getContext().createProtocol(NtceSvrHeartbeat.class.getSimpleName(), encode, 0, ntceSvrHeartbeat);
        return null;
    }
}
