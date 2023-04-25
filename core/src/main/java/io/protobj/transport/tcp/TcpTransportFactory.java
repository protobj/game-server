//package io.protobj.transport.tcp;
//
//import io.protobj.transport.Transport;
//import io.protobj.transport.TransportConfig;
//import io.protobj.transport.TransportFactory;
//import io.protobj.transport.TransportImpl;
//
//public final class TcpTransportFactory implements TransportFactory {
//
//  @Override
//  public Transport createTransport(TransportConfig config) {
//    return new TransportImpl(
//        config.messageCodec(),
//        new TcpReceiver(config),
//        new TcpSender(config),
//        config.addressMapper());
//  }
//}
