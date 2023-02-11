package org.protobj.mock.module.leagueSeizeble;

import com.guangyu.cd003.projects.gs.module.leagueSeizeble.msg.RespPassDoorCantPass;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespPassDoorCantPassHandler implements RespHandler<RespPassDoorCantPass> {

    @Override
    public void handle(MockConnect connect, RespPassDoorCantPass respMsg, int cmd) {
        //connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

        connect.LEGION_DATA.handle(respMsg);
    }

    @Override
    public int subCmd() {
        return 4101;
    }
}
