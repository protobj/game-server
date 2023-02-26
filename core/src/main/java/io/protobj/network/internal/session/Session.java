package io.protobj.network.internal.session;

import io.netty.channel.Channel;
import io.protobj.network.internal.message.BroadcastMessage;
import io.protobj.network.internal.message.MulticastMessage;
import io.protobj.network.internal.message.PushMessage;
import io.protobj.network.internal.message.UnicastMessage;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.Executor;

public abstract class Session {
    private int channelId;
    private volatile Executor executor;

    public void unicast(int index, Object msg) {
        getChannel().writeAndFlush(new UnicastMessage(channelId, index, msg));
    }

    public void broadcast(Object msg) {
        getChannel().writeAndFlush(new BroadcastMessage(msg));
    }

    public void multicast(List<Session> sessionList, Object msg) {
        if (CollectionUtils.isEmpty(sessionList)) {
            throw new IllegalArgumentException("sessionList is empty");
        }
        List<Integer> channelIds = sessionList.stream().map(it -> it.channelId).toList();
        getChannel().writeAndFlush(new MulticastMessage(channelIds, msg));
    }

    public void push(Object msg) {
        getChannel().writeAndFlush(new PushMessage(channelId, msg));
    }

    public Executor executor() {
        return executor;
    }

    public synchronized void setExecutor(Executor executor) {
        if (this.executor != null) {
            return;
        }
        this.executor = executor;
    }

    public abstract Channel getChannel();
}
