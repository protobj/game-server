package org.protobj.mock.module.npc;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.legion.msg.RespLegion;
import com.guangyu.cd003.projects.gs.module.npc.msg.RqstGetNpc;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.concurrent.CompletableFuture;

public class NpcController {

    public static CompletableFuture<Integer> searchNpc(RespLegion legion, MockConnect connect, int lv) {
        RqstGetNpc rqstGetNpc = new RqstGetNpc();
        rqstGetNpc.npclv = lv;
        if (legion != null) {
            rqstGetNpc.x = legion.x;
            rqstGetNpc.z = legion.z;
        }
        return connect.send(Commands.NPC_GET_CONST, rqstGetNpc);
    }
}
