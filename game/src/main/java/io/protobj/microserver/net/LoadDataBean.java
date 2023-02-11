package io.protobj.microserver.net;

import io.protobj.microserver.ServerType;

/**
 * Created on 2021/7/6.
 *
 * @author chen qiang
 */
public interface LoadDataBean {

    void load(int gid);

    void unload(int gid);

    ServerType ServerType();
}
