package io.protobj.services.transport.tcp;

import io.protobj.services.transport.Transport;
import io.protobj.services.transport.TransportConfig;
import io.protobj.services.transport.TransportFactory;
import io.protobj.services.transport.TransportImpl;

public final class TcpTransportFactory implements TransportFactory {

  @Override
  public Transport createTransport(TransportConfig config) {
    return new TransportImpl(
        config.messageCodec(),
        new TcpReceiver(config),
        new TcpSender(config),
        config.addressMapper());
  }
}
