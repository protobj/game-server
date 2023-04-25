package io.protobj.services.transport.rsocket;

import io.protobj.services.ServiceEndPoint;
import io.protobj.services.transport.api.*;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.scalecube.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RSocketClientTransport implements ClientTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketClientTransport.class);

    private final ThreadLocal<Map<Address, Mono<RSocket>>> rsockets =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private final HeadersCodec headersCodec;
    private final ContentCodec contentCodec;
    private final RSocketClientTransportFactory clientTransportFactory;

    /**
     * Constructor for this transport.
     *
     * @param headersCodec           headersCodec
     * @param contentCodec           dataCodecs
     * @param clientTransportFactory clientTransportFactory
     */
    public RSocketClientTransport(
            HeadersCodec headersCodec,
            ContentCodec contentCodec,
            RSocketClientTransportFactory clientTransportFactory) {
        this.headersCodec = headersCodec;
        this.contentCodec = contentCodec;
        this.clientTransportFactory = clientTransportFactory;
    }

    @Override
    public ClientChannel create(ServiceEndPoint endPoint) {
        final Map<Address, Mono<RSocket>> monoMap = rsockets.get(); // keep reference for threadsafety
        final Address address = endPoint.getAddress();
        Mono<RSocket> mono =
                monoMap.computeIfAbsent(
                        address,
                        key -> connect(key, monoMap)
                                .cache()
                                .doOnError(ex -> monoMap.remove(key)));
        return new RSocketClientChannel(mono, new MessageCodec(headersCodec, contentCodec));
    }

    private Mono<RSocket> connect(
            Address address, Map<Address, Mono<RSocket>> monoMap) {
        return RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .connect(() -> clientTransportFactory.clientTransport(address))
                .doOnSuccess(
                        rsocket -> {
                            LOGGER.debug("[rsocket][client][{}] Connected successfully", address);
                            // setup shutdown hook
                            rsocket
                                    .onClose()
                                    .doFinally(
                                            s -> {
                                                monoMap.remove(address);
                                                LOGGER.debug("[rsocket][client][{}] Connection closed", address);
                                            })
                                    .doOnError(
                                            th ->
                                                    LOGGER.warn(
                                                            "[rsocket][client][{}][onClose] Exception occurred: {}",
                                                            address,
                                                            th.toString()))
                                    .subscribe();
                        })
                .doOnError(
                        th ->
                                LOGGER.warn(
                                        "[rsocket][client][{}] Failed to connect, cause: {}", address, th.toString()));
    }
}
