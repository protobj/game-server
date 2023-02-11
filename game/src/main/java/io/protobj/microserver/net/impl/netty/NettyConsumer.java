package io.protobj.microserver.net.impl.netty;

import com.guangyu.cd003.projects.message.core.net.MQConsumer;
import com.guangyu.cd003.projects.message.core.net.MQProtocol;

public class NettyConsumer extends MQConsumer<MQProtocol> {

    InnerNettyConnector thisInnerNettyTcpConnector;

    public void recv(String producer, MQProtocol protocol) {
        getContext().recv(producer, protocol);
    }

    @Override
    public void close() {
        if (this.thisInnerNettyTcpConnector != null) {
            this.thisInnerNettyTcpConnector.unbindConsumer(this);
        }
    }

    public void bindInnerTcpServer(InnerNettyConnector thisInnerNettyTcpConnector) {
        thisInnerNettyTcpConnector.bindConsumer(this);
    }
}
