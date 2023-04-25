package io.protobj.services.transport.rsocket;

import io.protobj.services.ServiceEndPoint;
import io.protobj.services.transport.api.ContentCodec;
import io.protobj.services.transport.api.HeadersCodec;
import io.protobj.services.transport.api.ServerTransport;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.scalecube.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

public class RSocketServerTransport implements ServerTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketServerTransport.class);

    private final ServiceEndPoint endPoint;
    private final HeadersCodec headersCodec;
    private final ContentCodec contentCodec;
    private final RSocketServerTransportFactory serverTransportFactory;

    private CloseableChannel serverChannel; // calculated

    /**
     * Constructor for this server transport.
     * @param headersCodec           headersCodec
     * @param contentCodec           contentCodec
     * @param serverTransportFactory serverTransportFactory
     */
    public RSocketServerTransport(
            ServiceEndPoint endPoint,
            HeadersCodec headersCodec,
            ContentCodec contentCodec,
            RSocketServerTransportFactory serverTransportFactory) {
        this.endPoint = endPoint;
        this.headersCodec = headersCodec;
        this.contentCodec = contentCodec;
        this.serverTransportFactory = serverTransportFactory;
    }

    @Override
    public Address address() {
        InetSocketAddress socketAddress = serverChannel.address();
        return Address.create(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
    }

    @Override
    public Mono<ServerTransport> bind() {
        return Mono.defer(
                () ->
                        RSocketServer.create()
                                .acceptor(
                                        new RSocketServiceAcceptor(
                                                headersCodec,
                                                contentCodec,
                                                endPoint))
                                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                                .bind(serverTransportFactory.serverTransport())
                                .doOnSuccess(channel -> serverChannel = channel)
                                .thenReturn(this));
    }

    @Override
    public Mono<Void> stop() {
        return Mono.defer(
                () -> {
                    if (serverChannel == null || serverChannel.isDisposed()) {
                        return Mono.empty();
                    }
                    return Mono.fromRunnable(() -> serverChannel.dispose())
                            .then(
                                    serverChannel
                                            .onClose()
                                            .doOnError(
                                                    e ->
                                                            LOGGER.warn(
                                                                    "[rsocket][server][onClose] Exception occurred: {}",
                                                                    e.toString())));
                });
    }
}
