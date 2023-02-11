package io.protobj.microserver.net;

public class NetNotActiveException extends RuntimeException {
    public NetNotActiveException(Exception e) {
        super(e);
    }

    public NetNotActiveException() {
    }

    public NetNotActiveException(String message) {
        super(message);
    }
}
