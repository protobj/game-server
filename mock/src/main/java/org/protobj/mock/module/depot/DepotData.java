package org.protobj.mock.module.depot;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.guangyu.cd003.projects.gs.module.depot.msg.*;
import com.guangyu.cd003.projects.gs.module.item.cfg.FuncScrptItemCfg;
import com.guangyu.cd003.projects.gs.module.item.cfg.ItemCfg;
import com.pv.common.utilities.common.GsonUtil;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

public class DepotData {


    public Map<Integer, Integer> items = new HashMap<>();

    public Map<Integer, RespDepotEntities> entities = new HashMap<>();

    public List<MockItem> useItems = new ArrayList<>();

    static class MockItem {

        public ItemCfg cfg;

        public int num;

        public ItemCfg getCfg() {
            return cfg;
        }

        public void setCfg(FuncScrptItemCfg cfg) {
            this.cfg = cfg;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public MockItem(ItemCfg cfg, int num) {
            this.cfg = cfg;
            this.num = num;
        }
    }


    public void handle(RespDepotInfo respDepotInfo) {
        items.clear();
        entities.clear();
        if (respDepotInfo.items != null) {
            this.items = respDepotInfo.items;
        }
        if (respDepotInfo.entities != null) {
            this.entities = respDepotInfo.entities;
        }
        reset();
    }

    public void reset() {
        useItems.clear();
        this.items.forEach((k, v) -> {
            if (v <= 0) {
                return;
            }
            FuncScrptItemCfg cfg = CacheConfig.getCfg(FuncScrptItemCfg.class, k);
            HashSet<Integer> integers = Sets.newHashSet(
                    107040101,
                    107040102,
                    107040103,
                    107040104,
                    107040105,
                    107040106,
                    107040107
            );

            if (cfg != null && cfg.isUseDirectly() && !integers.contains(cfg.getId())) {
                useItems.add(new MockItem(cfg, v));
                return;
            }
            ItemCfg itemCfg = CacheConfig.getCfg(ItemCfg.class, k);
            if (itemCfg != null && itemCfg.isUseDirectly() && !integers.contains(itemCfg.getId())) {
                useItems.add(new MockItem(itemCfg, v));
            }
        });
    }

    public void handle(RespDepotOp respDepotOp) {

        if (CollectionUtils.isNotEmpty(respDepotOp.delEntities)) {
            for (Integer delEntity : respDepotOp.delEntities) {
                entities.remove(delEntity);
            }
        }
        if (MapUtils.isNotEmpty(respDepotOp.updItems)) {
            items.putAll(respDepotOp.updItems);
        }
        if (MapUtils.isNotEmpty(respDepotOp.updEntities)) {
            Map<Integer, RespDepotItemEntity> updEntities = respDepotOp.updEntities;

            RespDepotEntities respDepotEntities = entities.get(0);
            for (Integer ix : updEntities.keySet()) {
                respDepotEntities.entities.removeIf(it -> it.ix == ix);
            }
            respDepotEntities.entities.addAll(updEntities.values());
        }
        reset();
    }

    public void handle(RespProfitDepot respProfit) {

    }
}
