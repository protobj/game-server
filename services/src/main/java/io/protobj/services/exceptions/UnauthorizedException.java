package io.protobj.services.exceptions;

public class UnauthorizedException extends ServiceException {

  public static final int ERROR_TYPE = 401;

  public UnauthorizedException(int errorCode, String message) {
    super(errorCode, message);
  }

  public UnauthorizedException(String message) {
    super(ERROR_TYPE, message);
  }

  public UnauthorizedException(Throwable cause) {
    super(ERROR_TYPE, cause);
  }
}
