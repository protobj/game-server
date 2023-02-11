package org.protobj.mock.module.npc;

import com.guangyu.cd003.projects.gs.module.npc.msg.RespNpcInfo;
import com.guangyu.cd003.projects.mock.RespHandler;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class RespNpcInfoHandler implements RespHandler<RespNpcInfo> {

    @Override
    public void handle(MockConnect connect, RespNpcInfo respMsg, int cmd) {
        connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
    }

    @Override
    public int subCmd() {
        return 2001;
    }
}
