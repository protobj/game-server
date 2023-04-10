package transport;

public interface TransportFactory {

  Transport createTransport(TransportConfig config);
}
