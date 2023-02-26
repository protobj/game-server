package io.protobj.microserver;

import io.protobj.AServer;
import io.protobj.Module;
import io.protobj.msgdispatcher.MsgDispatcher;
import io.protobj.msgdispatcher.MsgDispatcherManager;
import io.protobj.network.Serializer;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.internal.NettyInternalClient;
import io.protobj.serializer.ProtobjSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Server extends AServer {

    private final ServerType serverType;

    private final int sid;
    private MsgDispatcher msgDispatcher;

    private Serializer serializer;

    public Server(ServerType serverType, int sid) {
        this.serverType = serverType;
        this.sid = sid;
    }

    @Override
    protected void preStart() {

    }

    @Override
    protected void postStart() {

    }

    @Override
    protected void initCustom(List<Module> modules) {
        super.initCustom(modules);
        this.msgDispatcher = new MsgDispatcherManager();
        this.msgDispatcher.init(modules, this);
        this.serializer = new ProtobjSerializer();
    }

    @Override
    protected void initNet() {

        NettyGateServer nettyGateServer = new NettyGateServer(1);
        String localhost = "localhost";
        int port = 9999;
        CompletableFuture<Void> gate = nettyGateServer.startTcpBackendServer(localhost, port);
        gate.whenCompleteAsync((r, e) -> {
            if (e != null) {
                e.printStackTrace();
            } else {
                System.err.printf("gate backend[tcp] start in %s:%d%n", localhost, port);
            }
        }).join();

        NettyInternalClient client = new NettyInternalClient(1, sessionManager(), msgDispatcher, serializer);



    }

    public ServerType getServerType() {
        return serverType;
    }


    public int getSid() {
        return sid;
    }

}
