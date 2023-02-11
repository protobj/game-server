package io.protobj.mock;


import io.protobj.mock.net.MockConnect;

import java.util.HashMap;
import java.util.Map;

public interface RespHandler<T> {

    Map<Integer, RespHandler> RESP_HANDLER_MAP = new HashMap<>();

    default void init() {
        RESP_HANDLER_MAP.put(this.subCmd(), this);
    }

    void handle(MockConnect connect, T respMsg, int cmd);


    default int subCmd() {
        return 0;
    }
}
