package io.protobj.microserver.serverregistry.zk;

import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;

public interface ServerListener {

    void addOrUpdate(ServerInfo serverInfo);

    void remove(ServerInfo serverInfo);
}
