package transport.tcp;

import io.netty.channel.ChannelOption;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;
import transport.Receiver;
import transport.TransportConfig;
import transport.TransportImpl;

public final class TcpReceiver implements Receiver {

  private final TransportConfig config;

  TcpReceiver(TransportConfig config) {
    this.config = config;
  }

  @Override
  public Mono<DisposableServer> bind() {
    return Mono.deferContextual(context -> Mono.just(context.get(TransportImpl.ReceiverContext.class)))
        .flatMap(
            context ->
                newTcpServer(context)
                    .handle((in, out) -> in.receive().retain().doOnNext(context::onMessage).then())
                    .bind()
                    .cast(DisposableServer.class));
  }

  private TcpServer newTcpServer(TransportImpl.ReceiverContext context) {
    return TcpServer.create()
        .runOn(context.loopResources())
        .port(config.port())
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.SO_REUSEADDR, true)
        .doOnChannelInit(
            (connectionObserver, channel, remoteAddress) ->
                new TcpChannelInitializer(config.maxFrameLength())
                    .accept(connectionObserver, channel));
  }
}
