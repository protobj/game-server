package io.protobj.microserver.net.impl.netty;


import io.protobj.microserver.net.MQConsumer;
import io.protobj.microserver.net.MQProtocol;

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
