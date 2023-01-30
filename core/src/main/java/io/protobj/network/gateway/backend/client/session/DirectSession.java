package io.protobj.network.gateway.backend.client.session;

import io.netty.channel.Channel;

public class DirectSession implements Session {
    private Channel channel;


    @Override
    public void sendMsg(Object msg) {

    }
}
