package io.protobj.exception;

import io.protobj.msg.RespError;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class LogicException extends RuntimeException {

    private final int code;

    public LogicException(int code) {
        this.code = code;
    }

    public LogicException(String message, int code) {
        super(message);
        this.code = code;
    }

    public static LogicException error(int code) {
        return new LogicException(code);
    }

    public static LogicException error(String message, int code) {
        return new LogicException(message, code);
    }

    public RespError toResp() {
        RespError respError = new RespError();
        respError.setCode(code);
        respError.setMessage(getMessage());
        String stackTrace = ExceptionUtils.getStackTrace(this);
        respError.setStackTrace(stackTrace);
        return respError;
    }
    public static RespError unknownErrorToResp(Throwable throwable){
        RespError respError = new RespError();
        respError.setCode(0);
        respError.setMessage(throwable.getMessage());
        String stackTrace = ExceptionUtils.getStackTrace(throwable);
        respError.setStackTrace(stackTrace);
        return respError;
    }
}
