package org.protobj.mock.module.scene.selector;

import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.gs.module.scenersrc.cons.SceneElemtTypeSceneRsrc;
import com.guangyu.cd003.projects.gs.module.scenersrc.msg.RespSceneElemtSceneRsrc;
import com.guangyu.cd003.projects.mock.module.scene.RespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.Comparator;
import java.util.List;

public class RsrcRespSceneElemtSelector implements RespSceneElemtSelector {

    public static final RespSceneElemtSelector INSTANCE = new RsrcRespSceneElemtSelector();

    private RsrcRespSceneElemtSelector() {
    }

    @Override
    public RespSceneElemt select(MockConnect connect, List<RespSceneElemt> elemts) {
        elemts.sort(new Comparator<RespSceneElemt>() {
            @Override
            public int compare(RespSceneElemt o1, RespSceneElemt o2) {
                RespSceneElemtSceneRsrc elemt1 = o1.as(RespSceneElemtSceneRsrc.class);
                RespSceneElemtSceneRsrc elemt2 = o2.as(RespSceneElemtSceneRsrc.class);
                return elemt1.uniqIxOfSeizor - elemt2.uniqIxOfSeizor;
            }

        });
        return elemts.get(0);
    }

    @Override
    public boolean filter(MockConnect mockConnect, RespSceneElemt elemt) {
        return elemt.type == SceneElemtTypeSceneRsrc.RSRC.getCode();
    }
}
