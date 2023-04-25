package io.protobj.services.transport.api;

import io.protobj.services.api.Message;

import java.util.function.BiFunction;

@FunctionalInterface
public interface ServiceMessageDataDecoder
        extends BiFunction<Message, Class<?>, Message> {
}
