package io.protobj.network.gateway.backend.client.session;

import io.netty.channel.Channel;

public class DirectSession extends Session {
    private Channel channel;

    @Override
    public void sendMsg(int index, Object msg) {

    }
}
