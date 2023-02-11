package org.protobj.mock;

import io.netty.channel.Channel;

public interface IMockContext {
    void onRegularSucc(Channel channel);

    void onEvent(Channel channel, Integer cmd, Object code);

    void removeConnect(Channel channel);

    default void onEventBeforeDecode(Channel channel, Integer cmd,int code){

    }
}
