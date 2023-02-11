package org.protobj.mock.module.legion;

import com.guangyu.cd003.projects.gs.module.legion.cons.RespTypeLegion;
import com.guangyu.cd003.projects.gs.module.legion.msg.RespLegionOp;
import com.guangyu.cd003.projects.mock.RespHandler;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class RespLegionOpHandler implements RespHandler<RespLegionOp> {

    @Override
    public void handle(MockConnect connect, RespLegionOp respMsg, int cmd) {
        //connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
        connect.LEGION_DATA.handle(respMsg);
    }

    @Override
    public int subCmd() {
        return RespTypeLegion.RESP_TYPE_LEGION_OP;
    }
}
