package org.protobj.mock.module.hero;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.hero.msg.RespHeroInfo;
import com.guangyu.cd003.projects.gs.module.hero.msg.RespHeroOp;
import com.guangyu.cd003.projects.gs.module.hero.msg.RespProfitHero;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class HeroData {
    public RespHeroInfo respHeroInfo;

    public void handle(RespHeroInfo respHeroInfo) {
        this.respHeroInfo = respHeroInfo;
    }

    public void handle(RespHeroOp respHeroOp) {
        if (this.respHeroInfo == null) {
            return;
        }
        if (respHeroOp.heroUpds != null) {
            this.respHeroInfo.heros.putAll(respHeroOp.heroUpds);
        }
    }

    public void handle(MockConnect connect, RespProfitHero respProfit) {
        connect.send(Commands.HERO_LOAD_CONST, null);
    }
}
