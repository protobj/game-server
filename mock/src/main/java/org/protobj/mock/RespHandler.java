package org.protobj.mock;

import com.guangyu.cd003.projects.common.msg.RespRawDataType;
import com.guangyu.cd003.projects.common.msg.RespRawDatalizable;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public interface RespHandler<T extends RespRawDatalizable> {

    Map<Integer, RespHandler> RESP_HANDLER_MAP = new HashMap<>();

    default void init() {
        RESP_HANDLER_MAP.put(this.subCmd(), this);
    }

    void handle(MockConnect connect, T respMsg, int cmd);


    default int subCmd() {
        return 0;
    }
}
