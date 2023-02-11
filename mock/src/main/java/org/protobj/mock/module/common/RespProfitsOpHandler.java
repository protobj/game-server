package org.protobj.mock.module.common;

import com.guangyu.cd003.projects.gs.module.common.msg.RespProfitsOp;
import com.guangyu.cd003.projects.mock.RespHandler;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class RespProfitsOpHandler implements RespHandler<RespProfitsOp> {

    @Override
    public void handle(MockConnect connect, RespProfitsOp respMsg, int cmd) {
        //connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
//		for (RespProfits value : respMsg.profitses.values()) {
//			for (RespProfit respProfit : value.profits.values()) {
//				if (respProfit instanceof RespProfitCity) {
//					connect.CITY_DATA.handle((RespProfitCity)respProfit);
//				} else if (respProfit instanceof RespProfitHero) {
//					connect.HERO_DATA.handle(connect,(RespProfitHero) respProfit);
//				}else if (respProfit instanceof RespProfitDepot){
//					connect.DEPOT_DATA.handle((RespProfitDepot) respProfit);
//				}
//			}
//		}
    }

    @Override
    public int subCmd() {
        return 9901;
    }
}
