package transport.tcp;

import transport.Transport;
import transport.TransportConfig;
import transport.TransportFactory;
import transport.TransportImpl;

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
