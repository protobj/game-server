package org.protobj.mock.module.chat;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.chat.cons.ChatType;
import com.guangyu.cd003.projects.gs.module.chat.msg.RqstChat;
import com.guangyu.cd003.projects.gs.module.hero.cfg.HeroCfg;
import com.guangyu.cd003.projects.gs.module.hero.msg.RespHero;
import com.guangyu.cd003.projects.gs.module.legion.cfg.LegionUnitCfg;
import com.guangyu.cd003.projects.mock.module.legion.LegionState;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.util.RandomUtils;
import io.netty.util.collection.IntObjectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatController {

    public static void worldChat(MockConnect connect) {
        RqstChat rqstChat = new RqstChat();
        rqstChat.channel = ChatType.WORLD.getValue();
        int cityx = connect.CITY_DATA.respCityInfo.x;
        int cityz = connect.CITY_DATA.respCityInfo.z;
        String cityPos = " pos：" + (cityx / 6) + " " + (cityz / 6) + " ";
        String legionStates = connect.LEGION_DATA.legionStates.values().stream().map(LegionState::getStateString).collect(Collectors.joining(","));
        rqstChat.msg = "msg from " + connect.getUid() + " " + cityPos + " " + legionStates;
        if (!connect.isSending(Commands.CHAT_SEND_MSG_CONST)) {
            connect.send(Commands.CHAT_SEND_MSG_CONST, rqstChat);
        }
    }

    public static CompletableFuture<Integer> corpsFull(MockConnect connect) {
        IntObjectMap<LegionUnitCfg> cfgsByClz = CacheConfig.getCfgsByClz(LegionUnitCfg.class);
        List<LegionUnitCfg> collect = cfgsByClz.values().stream().filter(it -> it.getSpd() > 100).collect(Collectors.toList());
        LegionUnitCfg random = RandomUtils.random(collect);
//        RqstChat rqstChat = new RqstChat();
//        rqstChat.channel = 10;
//        rqstChat.msg = "/cmd addCorps " + random.getId() + " " + 1000000;
//        return connect.send(Commands.CHAT_SEND_MSG_CONST, rqstChat);
        return connect.sendChatCMD("addCorps", random.getId(), 1000000);
    }

    public static CompletableFuture<Integer> sendAddHero(MockConnect connect) {
        IntObjectMap<HeroCfg> cfgsByClz = CacheConfig.getCfgsByClz(HeroCfg.class);
        Map<Integer, RespHero> integers = connect.HERO_DATA.respHeroInfo.heros;
        List<HeroCfg> heroCfgs = new ArrayList<>();
        for (HeroCfg value : cfgsByClz.values()) {
            if (value.getLvCfgs() == null) {
                continue;
            }
            if (value.getHeroSklPasvs() == null) {
                continue;
            }
            if (value.getHeroSklActvs() == null) {
                continue;
            }
            if (value.getHeroStarCfgs() == null || value.getHeroStarCfgs()[value.getInitStar()] == null) {
                continue;
            }
            heroCfgs.add(value);
        }
        List<HeroCfg> random = RandomUtils.random(heroCfgs, integers != null ? integers.size() + 1 : 1);
        if (random.size() > 1) {
            random.removeIf(it -> integers.containsKey(it.getId()));
        }
        if (random.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }
        return connect.sendChatCMD("addHero", random.get(0).getId(), 1);
    }
}
