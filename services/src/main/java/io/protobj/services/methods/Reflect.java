package io.protobj.services.methods;

import io.protobj.services.api.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static io.protobj.services.methods.CommunicationMode.*;

public class Reflect {

    private Reflect() {
    }

    public static CommunicationMode communicationMode(Method method) {
        Class<?> returnType = method.getReturnType();
        if (isRequestChannel(method)) {
            return REQUEST_CHANNEL;
        } else if (returnType.isAssignableFrom(Flux.class)) {
            return REQUEST_STREAM;
        } else if (returnType.isAssignableFrom(Mono.class)) {
            return REQUEST_RESPONSE;
        } else if (returnType.isAssignableFrom(Void.TYPE)) {
            return FIRE_AND_FORGET;
        } else if (Message.Content.class.isAssignableFrom(returnType)) {
            return REQUEST_RESPONSE_BLOCK;
        } else {
            throw new IllegalArgumentException(
                    "Service method is not supported (check return type or parameter type): " + method);
        }
    }

    private static boolean isRequestChannel(Method method) {
        Class<?>[] reqTypes = method.getParameterTypes();
        return reqTypes.length > 0
                && (Flux.class.isAssignableFrom(reqTypes[0]));
    }
}
