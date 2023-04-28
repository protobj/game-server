package io.protobj.services.methods;

import io.protobj.services.api.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

public interface MethodInvoker {

    int cmd();

    CommunicationMode mode();

    Type parameterType();

    default void invoke(Message.Content content) {
        throw new UnsupportedOperationException();
    }

    default Mono<Message.Content> invokeOne(Message.Content content) {
        throw new UnsupportedOperationException();
    }

    default Message.Content invokeOneBlock(Message.Content content) {
        return invokeOne(content).block();
    }

    default Flux<Message.Content> invokeMany(Message.Content content) {
        throw new UnsupportedOperationException();
    }

    default Flux<Message.Content> invokeBidirectional(Flux<Message.Content> content) {
        throw new UnsupportedOperationException();
    }

}
