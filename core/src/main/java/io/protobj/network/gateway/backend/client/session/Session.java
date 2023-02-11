package io.protobj.network.gateway.backend.client.session;

import io.netty.channel.Channel;

import java.util.concurrent.Executor;

public abstract class Session {
    private int channelId;
    private volatile Executor executor;

    public void sendMsg(int index, Object msg) {

    }

    public void sendMsg(Object msg) {
        sendMsg(0, msg);
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
