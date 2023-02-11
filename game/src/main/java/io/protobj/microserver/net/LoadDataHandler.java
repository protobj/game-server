package io.protobj.microserver.net;

import com.guangyu.cd003.projects.message.core.SvrType;

public interface LoadDataHandler {

    default void load(SvrType svrType, int gid) {
    }

    default void unload(SvrType svrType, int sid) {
    }
}
