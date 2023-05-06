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

    //    Mono<Message.Content>
    default Mono invokeOne(Message.Content content) {
        throw new UnsupportedOperationException();
    }

    default Message.Content invokeOneBlock(Message.Content content) {
        throw new UnsupportedOperationException();
    }

    //    Flux<Message.Content>
    default Flux invokeMany(Message.Content content) {
        throw new UnsupportedOperationException();
    }

    //    Flux<Message.Content>
    default Flux invokeBidirectional(Flux content) {
        throw new UnsupportedOperationException();
    }

}
