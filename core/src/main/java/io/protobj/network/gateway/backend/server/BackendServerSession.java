package io.protobj.network.gateway.backend.server;

import io.netty.channel.Channel;

public class BackendServerSession {

    private int sid;

    private Channel channel;

    public BackendServerSession(int sid, Channel channel) {
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
