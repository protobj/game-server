package io.protobj.services.transport.tcp;

import io.netty.channel.ChannelOption;
import io.protobj.services.transport.Message;
import io.protobj.services.transport.Sender;
import io.protobj.services.transport.TransportConfig;
import io.protobj.services.transport.TransportImpl;
import io.scalecube.net.Address;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

public final class TcpSender implements Sender {

    private final TransportConfig config;

    TcpSender(TransportConfig config) {
        this.config = config;
    }

    @Override
    public Mono<Connection> connect(Address address) {
        return Mono.deferContextual(context -> Mono.just(context.get(TransportImpl.SenderContext.class)))
                .map(context -> newTcpClient(context, address))
                .flatMap(TcpClient::connect);
    }

    @Override
    public Mono<Void> send(Message message) {
        return Mono.deferContextual(
                context -> {
                    Connection connection = context.get(Connection.class);
                    TransportImpl.SenderContext senderContext = context.get(TransportImpl.SenderContext.class);
                    return connection
                            .outbound()
                            .sendObject(Mono.just(message).map(senderContext.messageEncoder()), bb -> true)
                            .then();
                });
    }

    private TcpClient newTcpClient(TransportImpl.SenderContext context, Address address) {
        TcpClient tcpClient =
                TcpClient.newConnection()
                        .runOn(context.loopResources())
                        .host(address.host())
                        .port(address.port())
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeout())
                        .doOnChannelInit(
                                (connectionObserver, channel, remoteAddress) ->
                                        new TcpChannelInitializer(config.maxFrameLength())
                                                .accept(connectionObserver, channel));
        return config.isClientSecured() ? tcpClient.secure() : tcpClient;
    }
}
