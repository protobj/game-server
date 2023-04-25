package io.protobj.services.exceptions;

import io.protobj.services.api.Message;

import java.util.Optional;

public final class DefaultErrorMapper
        implements ServiceClientErrorMapper, ServiceProviderErrorMapper {

    public static final DefaultErrorMapper INSTANCE = new DefaultErrorMapper();

    private static final int DEFAULT_ERROR_CODE = 500;

    private DefaultErrorMapper() {
        // do not instantiate
    }

    @Override
    public Throwable toError(Message message) {

        Message.ErrorData data = (Message.ErrorData) message.getContent();

        int errorCode = data.getCode();
        String errorMessage = data.getDetail();

        switch (errorCode) {
            case BadRequestException.ERROR_TYPE:
                return new BadRequestException(errorCode, errorMessage);
            case UnauthorizedException.ERROR_TYPE:
                return new UnauthorizedException(errorCode, errorMessage);
            case ForbiddenException.ERROR_TYPE:
                return new ForbiddenException(errorCode, errorMessage);
            case ServiceUnavailableException.ERROR_TYPE:
                return new ServiceUnavailableException(errorCode, errorMessage);
            case InternalServiceException.ERROR_TYPE:
                return new InternalServiceException(errorCode, errorMessage);
            // Handle other types of Service Exceptions here
            default:
                return new InternalServiceException(errorCode, errorMessage);
        }
    }

    @Override
    public Message toMessage(Message.Header header, Throwable throwable) {
        int errorCode = DEFAULT_ERROR_CODE;
        if (throwable instanceof ServiceException) {
            errorCode = ((ServiceException) throwable).errorCode();
        }

        String errorMessage =
                Optional.ofNullable(throwable.getMessage()).orElseGet(throwable::toString);

        return Message.error(header, errorCode, errorMessage);
    }
}
