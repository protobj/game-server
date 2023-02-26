package io.protobj.network.gateway.external;

import io.netty.channel.Channel;

public class GateExternalSession {
    private int id;

    private int sid;

    private Channel channel;

    public GateExternalSession(int id, int sid, Channel channel) {
        this.id = id;
        this.sid = sid;
        this.channel = channel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
