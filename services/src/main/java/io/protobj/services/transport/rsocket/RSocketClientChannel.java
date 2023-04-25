package io.protobj.services.transport.rsocket;

import io.protobj.services.api.Message;
import io.protobj.services.exceptions.ConnectionClosedException;
import io.protobj.services.transport.api.ClientChannel;
import io.protobj.services.transport.api.MessageCodec;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;

import java.lang.reflect.Type;

public class RSocketClientChannel implements ClientChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketClientChannel.class);

    private final Mono<RSocket> rsocket;
    private final MessageCodec messageCodec;

    public RSocketClientChannel(Mono<RSocket> rsocket, MessageCodec codec) {
        this.rsocket = rsocket;
        this.messageCodec = codec;
    }

    @Override
    public Mono<Void> fireAndForget(Message message) {
        return rsocket.flatMap(rsocket -> rsocket.fireAndForget(toPayload(message)))
                .onErrorMap(RSocketClientChannel::mapConnectionAborted);
    }

    @Override
    public Mono<Message> requestResponse(Message message, Type responseType) {
        return rsocket
                .flatMap(rsocket -> rsocket.requestResponse(toPayload(message)))
                .map(payload -> this.toMessage(payload, responseType))
                .onErrorMap(RSocketClientChannel::mapConnectionAborted);
    }

    @Override
    public Flux<Message> requestStream(Message message, Type responseType) {
        return rsocket
                .flatMapMany(rsocket -> rsocket.requestStream(toPayload(message)))
                .map(payload -> this.toMessage(payload, responseType))
                .onErrorMap(RSocketClientChannel::mapConnectionAborted);
    }

    @Override
    public Flux<Message> requestChannel(
            Publisher<Message> publisher, Type responseType) {
        return rsocket
                .flatMapMany(rsocket -> rsocket.requestChannel(Flux.from(publisher).map(this::toPayload)))
                .map(payload -> this.toMessage(payload, responseType))
                .onErrorMap(RSocketClientChannel::mapConnectionAborted);
    }

    private Payload toPayload(Message request) {
        return messageCodec.encodeAndTransform(request, ByteBufPayload::create);
    }

    private Message toMessage(Payload payload, Type responseType) {
        try {
            return messageCodec.decodeContent(
                    messageCodec.decodeHeader(payload.sliceMetadata().retain()),
                    payload.sliceData().retain(),
                    responseType
            );
        } finally {
            payload.release();
        }
    }

    private static Throwable mapConnectionAborted(Throwable t) {
        return AbortedException.isConnectionReset(t) || ConnectionClosedException.isConnectionClosed(t)
                ? new ConnectionClosedException(t)
                : t;
    }
}
