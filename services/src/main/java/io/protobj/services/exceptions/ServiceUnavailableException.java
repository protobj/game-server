package io.protobj.services.exceptions;

public class ServiceUnavailableException extends ServiceException {

  public static final int ERROR_TYPE = 503;

  public ServiceUnavailableException(String message) {
    this(ERROR_TYPE, message);
  }

  public ServiceUnavailableException(int errorCode, String message) {
    super(errorCode, message);
  }

  public ServiceUnavailableException(Throwable cause) {
    super(ERROR_TYPE, cause);
  }
}
