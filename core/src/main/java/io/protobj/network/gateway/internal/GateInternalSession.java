package io.protobj.network.gateway.internal;

import io.netty.channel.Channel;

public class GateInternalSession {

    private int sid;

    private Channel channel;

    public GateInternalSession(int sid, Channel channel) {
        this.sid = sid;
        this.channel = channel;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getSid() {
        return sid;
    }
}
