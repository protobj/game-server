package io.protobj.microserver.net;

import io.protobj.microserver.serverregistry.ServerInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Created on 2021/6/23.
 *
 * @author chen qiang
 * 单个服务器对单个服务器发送消息
 */
public abstract class MQProducer<T> {

    private MQContext<T> context;

    private ServerInfo serverInfo;

    public abstract CompletableFuture<?> sendAsync(T msg);

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public abstract void close();

    public void setContext(MQContext<T> context) {
        this.context = context;
    }

    public MQContext<T> getContext() {
        return context;
    }

    public abstract void listenDestroy();

    public abstract boolean isClose();
}
