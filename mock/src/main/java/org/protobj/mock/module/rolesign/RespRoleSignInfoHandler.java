package org.protobj.mock.module.rolesign;

import com.guangyu.cd003.projects.gs.module.mapsign.cons.RespTypeMapSign;
import com.guangyu.cd003.projects.gs.module.mapsign.msg.RespMapSignInfo;
import com.guangyu.cd003.projects.mock.RespHandler;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class RespRoleSignInfoHandler implements RespHandler<RespMapSignInfo> {
    @Override
    public void handle(MockConnect connect, RespMapSignInfo respMsg, int cmd) {
        //connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
    }

    @Override
    public int subCmd() {
        return RespTypeMapSign.RESP_TYPE_MAPSIGN_INFO;
    }
}
