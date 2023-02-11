package io.protobj.microserver.net;

import io.protobj.microserver.ServerType;

public interface LoadDataHandler {

    default void load(ServerType ServerType, int gid) {
    }

    default void unload(ServerType ServerType, int sid) {
    }
}
