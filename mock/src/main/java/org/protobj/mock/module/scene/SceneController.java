package org.protobj.mock.module.scene;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.gs.module.scene.msg.RqstSceneElemt;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SceneController {


    public static CompletableFuture<Integer> rqstElemt(MockConnect connect, int lv, int x, int z) {
        RqstSceneElemt rqstSceneElemt = new RqstSceneElemt();
        rqstSceneElemt.sightLv = lv;
        rqstSceneElemt.x = x;
        rqstSceneElemt.z = z;
        if (connect.isSending(Commands.SCENE_RQST_ELEMT)) {
            return CompletableFuture.completedFuture(0);
        }
        return connect.send(Commands.SCENE_RQST_ELEMT, rqstSceneElemt);
    }

    public static CompletableFuture<Integer> rqstElemt(MockConnect connect, int lv, String tgtId, int rix) {
        RqstSceneElemt rqstSceneElemt = new RqstSceneElemt();
        rqstSceneElemt.sightLv = lv;
        rqstSceneElemt.tgtHolderId = tgtId;
        rqstSceneElemt.uniqIxOfTgt = rix;
        if (connect.isSending(Commands.SCENE_RQST_ELEMT_BY_TGT_CONST)) {
            return CompletableFuture.completedFuture(0);
        }
        return connect.send(Commands.SCENE_RQST_ELEMT_BY_TGT_CONST, rqstSceneElemt);
    }

    public static RespSceneElemt findElemt(MockConnect connect, RespSceneElemtSelector sceneElemtSelector) {

        Map<Integer, RespSceneElemt> respSceneElemtMap = connect.SCENE_DATA.respSceneElemtMap;
        List<RespSceneElemt> elemts = new ArrayList<>();
        for (RespSceneElemt respSceneElemt : respSceneElemtMap.values()) {
            if (sceneElemtSelector.filter(connect, respSceneElemt)) {
                elemts.add(respSceneElemt);
            }
        }
        if (elemts.isEmpty()) {
            return null;
        }
        return sceneElemtSelector.select(connect, elemts);
    }
}
