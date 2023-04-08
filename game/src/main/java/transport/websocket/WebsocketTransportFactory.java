package transport.websocket;

import io.scalecube.cluster.transport.api.Transport;
import io.scalecube.cluster.transport.api.TransportConfig;
import io.scalecube.cluster.transport.api.TransportFactory;
import io.scalecube.transport.netty.TransportImpl;
import io.scalecube.transport.netty.websocket.WebsocketReceiver;
import io.scalecube.transport.netty.websocket.WebsocketSender;

public final class WebsocketTransportFactory implements TransportFactory {

  @Override
  public Transport createTransport(TransportConfig config) {
    return new TransportImpl(
        config.messageCodec(),
        new WebsocketReceiver(config),
        new WebsocketSender(config),
        config.addressMapper());
  }
}
