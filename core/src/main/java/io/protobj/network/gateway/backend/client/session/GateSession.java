package io.protobj.network.gateway.backend.client.session;

import io.netty.channel.Channel;

import java.util.concurrent.Executor;

public class GateSession implements Session {

    private int channelId;

    private MutilChannelSession session;

    private Executor executor;


    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public MutilChannelSession getSession() {
        return session;
    }

    public void setSession(MutilChannelSession session) {
        this.session = session;
    }

    @Override
    public void sendMsg(Object msg) {
        Channel channel = session.choose();
    }

    @Override
    public Executor executor() {
        return executor;
    }
}
