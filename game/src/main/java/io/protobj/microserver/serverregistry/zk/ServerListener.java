package io.protobj.microserver.serverregistry.zk;

import io.protobj.microserver.serverregistry.ServerInfo;

public interface ServerListener {

    void addOrUpdate(ServerInfo serverInfo);

    void remove(ServerInfo serverInfo);
}
