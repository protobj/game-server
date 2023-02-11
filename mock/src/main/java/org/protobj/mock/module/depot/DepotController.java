package org.protobj.mock.module.depot;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.item.msg.RqstUseItem;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.List;

public class DepotController {


    public static void use(MockConnect mockConnect) {
        List<DepotData.MockItem> useItems = mockConnect.DEPOT_DATA.useItems;
        if (useItems.isEmpty()) {
            mockConnect.DEPOT_DATA.reset();
        }
        if (useItems.isEmpty()) {
            return;
        }
        DepotData.MockItem mockItem = useItems.remove(0);
        RqstUseItem rqstUseItem = new RqstUseItem();
        rqstUseItem.cid = mockItem.cfg.getId();
        rqstUseItem.num = mockItem.num;
        mockConnect.send(Commands.ITEM_USE_CONST, rqstUseItem).exceptionally(e -> {
            e.getCause().printStackTrace();
            return null;
        });
    }
}
