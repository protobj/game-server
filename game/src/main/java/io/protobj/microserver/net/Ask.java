package io.protobj.microserver.net;

import io.netty.util.internal.ObjectPool;
import io.protobj.microserver.ServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class Ask<A> {
    private static ObjectPool<Ask> ASK_OBJ_POOL_2 = ObjectPool.newPool(Ask::new);

    private static final long ASK_TIMEOUT = 3000;

    public Ask(ObjectPool.Handle<Ask> handle) {
        this.handle = handle;
    }

    public Ask() {
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > ASK_TIMEOUT;
    }

    public static Ask createAsk(Object rqst, CompletableFuture future, ServerType ServerType) {
        Ask<?> ask = ASK_OBJ_POOL_2.get();
        ask.ask = rqst;
        ask.future = future;
        ask.startTime = System.currentTimeMillis();
        ask.askServerType = ServerType;
        return ask;
    }

    private static final Logger logger = LoggerFactory.getLogger(Ask.class);
    Object ask;
    private CompletableFuture<A> future;
    private long startTime;
    private ObjectPool.Handle<Ask> handle;
    private ServerType askServerType;

    public void complete(A result) {
        future.complete(result);
        if (logger.isDebugEnabled()) {
            logger.debug("请求complete：{}  耗时：{}ms", ask.getClass().getSimpleName(), System.currentTimeMillis() - startTime);
        }
    }

    public void completeExceptionally(Throwable result) {
        future.completeExceptionally(result);
        logger.error("请求completeExceptionally：{} 耗时：{}ms", ask.getClass().getSimpleName(), System.currentTimeMillis() - startTime);
    }

    public CompletableFuture<A> getFuture() {
        return future;
    }

    public ServerType getAskServerType() {
        return askServerType;
    }

    public void setAsk(Object ask) {
        this.ask = ask;
    }

    public void recycle() {
        this.ask = null;
        this.future = null;
        this.startTime = 0;
        if (handle != null) {
            handle.recycle(this);
        }
    }

}
