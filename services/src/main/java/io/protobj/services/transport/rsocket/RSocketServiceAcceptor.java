package io.protobj.services.transport.rsocket;

import io.netty.buffer.ByteBuf;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.api.Message;
import io.protobj.services.exceptions.BadRequestException;
import io.protobj.services.exceptions.ServiceException;
import io.protobj.services.exceptions.ServiceUnavailableException;
import io.protobj.services.methods.ServiceMethodInvoker;
import io.protobj.services.transport.api.ContentCodec;
import io.protobj.services.transport.api.HeadersCodec;
import io.protobj.services.transport.api.MessageCodec;
import io.protobj.services.transport.api.ReferenceCountUtil;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.ByteBufPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

public class RSocketServiceAcceptor implements SocketAcceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketServiceAcceptor.class);

    private final HeadersCodec headersCodec;
    private final ContentCodec contentCodec;
    private final ServiceEndPoint endPoint;

    /**
     * Constructor.
     *
     * @param headersCodec headersCodec
     * @param contentCodec dataCodecs
     * @param endPoint     methodRegistry
     */
    public RSocketServiceAcceptor(
            HeadersCodec headersCodec,
            ContentCodec contentCodec,
            ServiceEndPoint endPoint) {
        this.headersCodec = headersCodec;
        this.contentCodec = contentCodec;
        this.endPoint = endPoint;
    }

    @Override
    public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket rsocket) {
        LOGGER.info("[rsocket][accept][{}] setup: {}", rsocket, setupPayload);
        return Mono.fromCallable(this::newRSocket);
    }

    private RSocket newRSocket() {
        return new RSocketImpl(new MessageCodec(headersCodec, contentCodec), endPoint);
    }

    private static class RSocketImpl implements RSocket {

        private final MessageCodec messageCodec;
        private final ServiceEndPoint endPoint;

        private RSocketImpl(MessageCodec messageCodec, ServiceEndPoint endPoint) {
            this.messageCodec = messageCodec;
            this.endPoint = endPoint;
        }

        @Override
        public Mono<Payload> requestResponse(Payload payload) {
            return Mono.defer(() -> Mono.just(toMessage(payload)))
                    .doOnNext(this::validateRequest)
                    .flatMap(
                            message -> {
                                ServiceMethodInvoker methodInvoker = endPoint.getInvoker(message.getHeader().getCmd());
                                validateMethodInvoker(methodInvoker, message);
                                return methodInvoker.invokeOne(message);
                            })
                    .map(this::toPayload)
                    .doOnError(ex -> LOGGER.error("[requestResponse][error] cause: {}", ex.toString()));
        }

        @Override
        public Flux<Payload> requestStream(Payload payload) {
            return Mono.defer(() -> Mono.just(toMessage(payload)))
                    .doOnNext(this::validateRequest)
                    .flatMapMany(
                            message -> {
                                ServiceMethodInvoker methodInvoker = endPoint.getInvoker(message.getHeader().getCmd());
                                validateMethodInvoker(methodInvoker, message);
                                return methodInvoker
                                        .invokeMany(message);
                            })
                    .map(this::toPayload)
                    .doOnError(ex -> LOGGER.error("[requestStream][error] cause: {}", ex.toString()));
        }

        @Override
        public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
            return Flux.defer(() -> Flux.from(payloads))
                    .map(this::toMessage)
                    .switchOnFirst(
                            (first, messages) -> {
                                if (first.hasValue()) {
                                    Message message = first.get();
                                    validateRequest(message);
                                    ServiceMethodInvoker methodInvoker = endPoint.getInvoker(message.getHeader().getCmd());
                                    return methodInvoker
                                            .invokeBidirectional(messages);
                                }
                                return messages;
                            })
                    .map(this::toPayload)
                    .doOnError(ex -> LOGGER.error("[requestChannel][error] cause: {}", ex.toString()));
        }

        private Payload toPayload(Message response) {
            return messageCodec.encodeAndTransform(response, ByteBufPayload::create);
        }

        private Message toMessage(Payload payload) {
            try {
                ByteBuf contentBuffer = payload.sliceData().retain();
                ByteBuf headersBuffer = payload.sliceMetadata().retain();
                Message.Header header = messageCodec.decodeHeader(headersBuffer);
                ServiceMethodInvoker invoker = endPoint.getInvoker(header.getCmd());
                Type paramType = invoker.getParamType();
                return messageCodec.decodeContent(header, contentBuffer, paramType);
            } finally {
                payload.release();
            }
        }

        private void validateRequest(Message message) throws ServiceException {
            if (message.getHeader().getCmd() == 0) {
                LOGGER.error("Qualifier is null, invocation failed for {}", message);
                throw new BadRequestException("Qualifier is null");
            }
        }

        private void validateMethodInvoker(ServiceMethodInvoker methodInvoker, Message message) {
            if (methodInvoker == null) {
                LOGGER.error("No service invoker found, invocation failed for {}", message);
                throw new ServiceUnavailableException("No service invoker found");
            }
        }

    }
}
