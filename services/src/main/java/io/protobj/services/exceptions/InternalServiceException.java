package io.protobj.services.exceptions;

public class InternalServiceException extends ServiceException {

  public static final int ERROR_TYPE = 500;

  public InternalServiceException(int errorCode, String message) {
    super(errorCode, message);
  }

  public InternalServiceException(Throwable cause) {
    super(ERROR_TYPE, cause);
  }

  public InternalServiceException(String message) {
    super(ERROR_TYPE, message);
  }

  public InternalServiceException(String message, Throwable cause) {
    super(ERROR_TYPE, message, cause);
  }
}
