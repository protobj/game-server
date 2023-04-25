package io.protobj.services.exceptions;

import io.protobj.services.api.Message;

@FunctionalInterface
public interface ServiceClientErrorMapper {

    /**
     * Maps service message to an exception.
     *
     * @param message the message to map to an exception.
     * @return an exception mapped from qualifier and error data.
     */
    Throwable toError(Message message);
}
