package io.protobj.network.gateway.backend.client.session;

import io.netty.channel.Channel;

public class GateSession extends Session {

    private MutilChannelSession session;

    public MutilChannelSession getSession() {
        return session;
    }

    public void setSession(MutilChannelSession session) {
        this.session = session;
    }

    @Override
    public Channel getChannel() {
        return session.choose();
    }
}
