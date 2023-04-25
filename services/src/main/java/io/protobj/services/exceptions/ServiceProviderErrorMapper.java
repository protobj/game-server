package io.protobj.services.exceptions;

import io.protobj.services.api.Message;

@FunctionalInterface
public interface ServiceProviderErrorMapper {

    /**
     * Maps an exception to a {@link io.protobj.services.api.Message}.
     *
     * @param throwable the exception to map to a service message.
     * @return a service message mapped from the supplied exception.
     */
    Message toMessage(Message.Header header, Throwable throwable);
}
