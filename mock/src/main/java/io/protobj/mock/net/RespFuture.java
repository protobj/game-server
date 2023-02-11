package io.protobj.mock.net;


import java.util.concurrent.CompletableFuture;

public class RespFuture<T> extends CompletableFuture<T> {

    public void completeExceptionally(int cmd, int code) {
        completeExceptionally(new CodeException(cmd, code));
    }
}
