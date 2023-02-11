package org.protobj.mock.module.product;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.product.msg.RqstProductHavest;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ProductController {


    public static void collect(MockConnect connect) {
        ProductData product_data = connect.PRODUCT_DATA;
        Set<Integer> rsrcTypes = product_data.rsrcTypes;
        if (rsrcTypes.isEmpty()) {
            product_data.resetRsrcTypes();
        }
        rsrcTypes = product_data.rsrcTypes;
        if (rsrcTypes.isEmpty()) {
            return;
        }
        Iterator<Integer> iterator = rsrcTypes.iterator();
        collect0(connect, iterator.next());
        iterator.remove();
    }

    public static CompletableFuture<Integer> collect0(MockConnect connect, int rsrcType) {
        RqstProductHavest rqstProductHavest = new RqstProductHavest();
        rqstProductHavest.rsrcType = rsrcType;
        rqstProductHavest.buildingRix = -1;
        return connect.send(Commands.PRODUCT_HARVEST_CONST, rqstProductHavest);
    }
}
