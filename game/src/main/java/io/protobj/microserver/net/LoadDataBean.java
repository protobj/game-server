package io.protobj.microserver.net;

import com.guangyu.cd003.projects.message.core.SvrType;

/**
 * Created on 2021/7/6.
 *
 * @author chen qiang
 */
public interface LoadDataBean {

    void load(int gid);

    void unload(int gid);

    SvrType svrType();
}
