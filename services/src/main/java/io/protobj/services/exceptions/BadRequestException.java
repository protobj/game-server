package io.protobj.services.exceptions;

public class BadRequestException extends ServiceException {

  public static final int ERROR_TYPE = 400;

  public BadRequestException(String message) {
    this(ERROR_TYPE, message);
  }

  public BadRequestException(int errorCode, String message) {
    super(errorCode, message);
  }

  public BadRequestException(Throwable cause) {
    super(ERROR_TYPE, cause);
  }
}
