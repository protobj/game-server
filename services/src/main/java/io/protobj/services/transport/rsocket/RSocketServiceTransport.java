package io.protobj.services.transport.rsocket;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.exceptions.ConnectionClosedException;
import io.protobj.services.transport.api.*;
import io.protobj.services.transport.codec.ProtostuffCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.FutureMono;
import reactor.netty.channel.AbortedException;
import reactor.netty.resources.LoopResources;

import java.util.StringJoiner;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

public class RSocketServiceTransport implements ServiceTransport {

    public static final Logger LOGGER = LoggerFactory.getLogger(RSocketServiceTransport.class);

    static {
        Hooks.onErrorDropped(
                t -> {
                    if (AbortedException.isConnectionReset(t)
                            || ConnectionClosedException.isConnectionClosed(t)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Connection aborted: {}", t.toString());
                        }
                    }
                });
    }

    private int numOfWorkers = Runtime.getRuntime().availableProcessors();

    private HeadersCodec headersCodec = new ProtostuffCodec();
    private ContentCodec contentCodec = (ContentCodec) headersCodec;
    private Function<LoopResources, RSocketServerTransportFactory> serverTransportFactory =
            RSocketServerTransportFactory.tcp();
    private Function<LoopResources, RSocketClientTransportFactory> clientTransportFactory =
            RSocketClientTransportFactory.tcp();

    // resources
    private EventLoopGroup eventLoopGroup;
    private LoopResources clientLoopResources;
    private LoopResources serverLoopResources;

    /**
     * Default constructor.
     */
    public RSocketServiceTransport() {
    }

    /**
     * Copy constructor.
     *
     * @param other other instance
     */
    private RSocketServiceTransport(RSocketServiceTransport other) {
        this.numOfWorkers = other.numOfWorkers;
        this.headersCodec = other.headersCodec;
        this.contentCodec = other.contentCodec;
        this.eventLoopGroup = other.eventLoopGroup;
        this.clientLoopResources = other.clientLoopResources;
        this.serverLoopResources = other.serverLoopResources;
        this.serverTransportFactory = other.serverTransportFactory;
        this.clientTransportFactory = other.clientTransportFactory;
    }

    /**
     * Setter for {@code numOfWorkers}.
     *
     * @param numOfWorkers number of worker threads
     * @return new {@code RSocketServiceTransport} instance
     */
    public RSocketServiceTransport numOfWorkers(int numOfWorkers) {
        RSocketServiceTransport rst = new RSocketServiceTransport(this);
        rst.numOfWorkers = numOfWorkers;
        return rst;
    }

    /**
     * Setter for {@code headersCodec}.
     *
     * @param headersCodec headers codec
     * @return new {@code RSocketServiceTransport} instance
     */
    public RSocketServiceTransport headersCodec(HeadersCodec headersCodec) {
        RSocketServiceTransport rst = new RSocketServiceTransport(this);
        rst.headersCodec = headersCodec;
        return rst;
    }

    /**
     * Setter for {@code dataCodecs}.
     *
     * @param dataCodecs set of data codecs
     * @return new {@code RSocketServiceTransport} instance
     */
    public RSocketServiceTransport dataCodecs(ContentCodec dataCodecs) {
        RSocketServiceTransport rst = new RSocketServiceTransport(this);
        rst.contentCodec = dataCodecs;
        return rst;
    }

    /**
     * Setter for {@code serverTransportFactory}.
     *
     * @param serverTransportFactory serverTransportFactory
     * @return new {@code RSocketServiceTransport} instance
     */
    public RSocketServiceTransport serverTransportFactory(
            Function<LoopResources, RSocketServerTransportFactory> serverTransportFactory) {
        RSocketServiceTransport rst = new RSocketServiceTransport(this);
        rst.serverTransportFactory = serverTransportFactory;
        return rst;
    }

    /**
     * Setter for {@code clientTransportFactory}.
     *
     * @param clientTransportFactory clientTransportFactory
     * @return new {@code RSocketServiceTransport} instance
     */
    public RSocketServiceTransport clientTransportFactory(
            Function<LoopResources, RSocketClientTransportFactory> clientTransportFactory) {
        RSocketServiceTransport rst = new RSocketServiceTransport(this);
        rst.clientTransportFactory = clientTransportFactory;
        return rst;
    }

    @Override
    public ClientTransport clientTransport() {
        return new RSocketClientTransport(
                headersCodec,
                contentCodec,
                clientTransportFactory.apply(clientLoopResources));
    }

    @Override
    public ServerTransport serverTransport(ServiceEndPoint localEndPoint) {
        return new RSocketServerTransport(
                localEndPoint,
                headersCodec,
                contentCodec,
                serverTransportFactory.apply(serverLoopResources));
    }

    @Override
    public Mono<RSocketServiceTransport> start() {
        return Mono.fromRunnable(this::start0).thenReturn(this);
    }

    @Override
    public Mono<Void> stop() {
        return Flux.concatDelayError(
                        Mono.defer(() -> serverLoopResources.disposeLater()),
                        Mono.defer(this::shutdownEventLoopGroup))
                .then();
    }

    private void start0() {
        eventLoopGroup = newEventLoopGroup();
        clientLoopResources = DelegatedLoopResources.newClientLoopResources(eventLoopGroup);
        serverLoopResources = DelegatedLoopResources.newServerLoopResources(eventLoopGroup);
    }

    private EventLoopGroup newEventLoopGroup() {
        ThreadFactory threadFactory = new DefaultThreadFactory("rsocket-worker", true);
        EventLoopGroup eventLoopGroup =
                Epoll.isAvailable()
                        ? new EpollEventLoopGroup(numOfWorkers, threadFactory)
                        : new NioEventLoopGroup(numOfWorkers, threadFactory);
        return LoopResources.colocate(eventLoopGroup);
    }

    private Mono<Void> shutdownEventLoopGroup() {
        //noinspection unchecked,rawtypes
        return Mono.defer(() -> FutureMono.from((Future) eventLoopGroup.shutdownGracefully()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RSocketServiceTransport.class.getSimpleName() + "[", "]")
                .add("numOfWorkers=" + numOfWorkers)
                .add("headersCodec=" + headersCodec)
                .add("dataCodecs=" + contentCodec)
                .add("serverTransportFactory=" + serverTransportFactory)
                .add("clientTransportFactory=" + clientTransportFactory)
                .toString();
    }
}
