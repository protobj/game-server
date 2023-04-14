package io.protobj.services.transport;

public interface TransportFactory {

  Transport createTransport(TransportConfig config);
}
