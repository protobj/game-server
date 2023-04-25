package io.protobj.services.transport.api;

import io.protobj.services.api.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

public interface ClientChannel {
    Mono<Void> fireAndForget(Message message);

    Mono<Message> requestResponse(Message message, Type responseType);

    Flux<Message> requestStream(Message message, Type responseType);

    Flux<Message> requestChannel(Publisher<Message> publisher, Type responseType);
}
