package org.protobj.mock.module.product;

import com.guangyu.cd003.projects.gs.module.product.cfg.ProductCfg;
import com.guangyu.cd003.projects.gs.module.product.msg.RespProduct;
import com.guangyu.cd003.projects.gs.module.product.msg.RespProductInfo;
import com.guangyu.cd003.projects.gs.module.product.msg.RespProductOp;
import com.pv.framework.gs.core.cfg.core.CacheConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductData {
    public Map<Integer, RespProduct> products = new HashMap<>();

    public Set<Integer> rsrcTypes = new HashSet<>();

    public void handle(RespProductInfo respProductInfo) {
        this.products = respProductInfo.products.stream().collect(Collectors.toMap(t -> t.rix, t -> t));
    }

    public void resetRsrcTypes() {
        rsrcTypes.clear();
        for (RespProduct product : products.values()) {
            ProductCfg cfg = CacheConfig.getCfg(ProductCfg.class, product.cid);
            rsrcTypes.add(cfg.getRsrcType());
        }
    }

    public void handle(RespProductOp respProductOp) {
        if (respProductOp.productUpds != null) {
            products.putAll(respProductOp.productUpds);
        }
    }
}
