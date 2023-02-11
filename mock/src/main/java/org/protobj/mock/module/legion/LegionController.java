package org.protobj.mock.module.legion;

import com.google.common.collect.Lists;
import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.city.cons.SceneElemtTypeCity;
import com.guangyu.cd003.projects.gs.module.city.msg.RespCanAtkNpcLv;
import com.guangyu.cd003.projects.gs.module.hero.cfg.DscrDataHero;
import com.guangyu.cd003.projects.gs.module.hero.cfg.HeroCfg;
import com.guangyu.cd003.projects.gs.module.hero.cfg.HeroLvCfg;
import com.guangyu.cd003.projects.gs.module.hero.msg.RespHero;
import com.guangyu.cd003.projects.gs.module.hero.msg.RespHeroInfo;
import com.guangyu.cd003.projects.gs.module.legion.cons.LegionActionType;
import com.guangyu.cd003.projects.gs.module.legion.cons.SceneElemtTypeLegion;
import com.guangyu.cd003.projects.gs.module.legion.msg.*;
import com.guangyu.cd003.projects.gs.module.legion.msg.state.RespLegionStatePathMv;
import com.guangyu.cd003.projects.gs.module.npc.cons.NpcType;
import com.guangyu.cd003.projects.gs.module.npc.msg.RespNpcInfo;
import com.guangyu.cd003.projects.gs.module.scene.msg.CodeScene;
import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.gs.module.scenersrc.cons.SceneElemtTypeSceneRsrc;
import com.guangyu.cd003.projects.mock.module.chat.ChatController;
import com.guangyu.cd003.projects.mock.module.npc.NpcController;
import com.guangyu.cd003.projects.mock.module.scene.RespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.module.scene.SceneController;
import com.guangyu.cd003.projects.mock.module.scene.selector.NpcRespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.module.scene.selector.RsrcRespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.net.CodeException;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.util.RandomUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LegionController {
    private static final Logger logger = LoggerFactory.getLogger(LegionController.class);


    private static void legionStateSwitch(MockConnect connect, LegionState legionState) {
        if (legionState.getCurState() == LegionState.LEGION_ON_CALC_ING) {
            return;
        }
        if (legionState.isIdle()) {
            return;
        }

        RespLegion next = legionState.legion;
        if (legionState.getCurState() == LegionActionType.RETURN.getType()) {
            if (connect.LEGION_DATA.unitCount(next) > connect.LEGION_DATA.initUnitCount(next) / 2 && next.actionType != LegionActionType.SEIZE_SCENE_RSRC.getType()) {
                //打怪回城时马上发起下一轮攻击
                if (legionState.stateStartTime + TimeUnit.SECONDS.toMillis(60) < System.currentTimeMillis()) {
                    legionState.setState(LegionActionType.WAIT.getType());
                }
            }
        } else if (next.actionType == LegionActionType.SEIZE_SCENE_RSRC.getType()) {
            //采集资源N秒后回城
            if (legionState.stateStartTime + TimeUnit.MINUTES.toMillis(10) < System.currentTimeMillis()) {
                retCity(connect, next);
                legionState.setState(LegionActionType.RETURN.getType());
            }
        }
    }

    private static void retCity(MockConnect connect, RespLegion next) {
        RqstLegionRetunCity rqstLegionRetunCity = new RqstLegionRetunCity();
        rqstLegionRetunCity.rix = next.uniqIxOfHolder;
        rqstLegionRetunCity.curX = next.x * 100;
        rqstLegionRetunCity.curZ = next.z * 100;
        connect.send(Commands.LEGION_RETURN_CITY_CONST, rqstLegionRetunCity);
    }

    private static CompletableFuture<Integer> fight(MockConnect connect, RespLegion next, RespSceneElemt tgtElmet) {
        if (tgtElmet.type == SceneElemtTypeSceneRsrc.RSRC.getCode()) {
            RqstLegionSeize rqstLegionSeize = new RqstLegionSeize();
            rqstLegionSeize.rix = next.uniqIxOfHolder;
            rqstLegionSeize.tgtSix = tgtElmet.six;
            rqstLegionSeize.curX = connect.CITY_DATA.respCityInfo.x;
            rqstLegionSeize.curZ = connect.CITY_DATA.respCityInfo.z;
            rqstLegionSeize.force = false;
            return connect.send(Commands.LEGION_SEIZE_SCENE_RSRC_CONST, rqstLegionSeize);
        } else if (tgtElmet.type == SceneElemtTypeLegion.LEGION.getCode()
                || tgtElmet.type == SceneElemtTypeLegion.NPC_LEGION.getCode()
                || tgtElmet.type == SceneElemtTypeLegion.UNION_LEGION.getCode()
                || tgtElmet.type == SceneElemtTypeCity.CITY.getCode()
        ) {
            RqstLegionAtkTgt atkAct = new RqstLegionAtkTgt();
            atkAct.rix = next.uniqIxOfHolder;
            atkAct.tgtSix = tgtElmet.six;
            atkAct.curX = next.x;
            atkAct.curZ = next.z;
            return connect.send(Commands.LEGION_ATK_TGT_CONST, atkAct);
        }
        throw new UnsupportedOperationException("未定义攻击目标：" + tgtElmet.type);
    }


    public static CompletableFuture<Integer> createLegion(MockConnect connect, RespSceneElemt tgtElmet) {
        Collection<RespHero> integers = availableHeros(connect);
        if (integers.isEmpty()) {
            connect.send(Commands.HERO_LOAD_CONST, null);
            return ChatController.sendAddHero(connect);
        }
        RqstLegionCre rqstLegionCre = new RqstLegionCre();
        List<RespHero> random = RandomUtils.random(integers, 2);
        int i = RandomUtils.nextInt(random.size() > 1 ? 2 : 1);
        RespHero cmmder = random.get(i);
        rqstLegionCre.cmmder = cmmder.cid;
        if (random.size() > 1 && cmmder.lv >= DscrDataHero.CMMDER_LV_OF_DEPUTY_UNLOCK.first()) {
            rqstLegionCre.deputy = random.get(i == 0 ? 1 : 0).cid;
        }
        rqstLegionCre.units = new HashMap<>();
        Map<Integer, Integer> corps = connect.CITY_DATA.getAvailableCorps();
        CompletableFuture<Integer> corpsFuture = new CompletableFuture<>();
        if (MapUtils.isEmpty(corps)) {
            return ChatController.corpsFull(connect);
        } else {
            corpsFuture.complete(0);
        }
        return corpsFuture.thenComposeAsync((t) -> {
            Map.Entry<Integer, Integer> corp = RandomUtils.random(connect.CITY_DATA.getAvailableCorps().entrySet());
            HeroCfg cfg = CacheConfig.getCfg(HeroCfg.class, rqstLegionCre.cmmder);
            HeroLvCfg lvCfg = cfg.getLvCfgs()[cmmder.lv];
            rqstLegionCre.units.put(corp.getKey(), Math.min(lvCfg.getLeadership() + connect.CITY_DATA.getBaseLegionLeaderShip(), corp.getValue()));
            if (tgtElmet.type == SceneElemtTypeSceneRsrc.RSRC.getCode()) {
                RqstLegionSeize rqstLegionSeize = new RqstLegionSeize();
                rqstLegionSeize.tgtSix = tgtElmet.six;
                rqstLegionSeize.curX = connect.CITY_DATA.respCityInfo.x;
                rqstLegionSeize.curZ = connect.CITY_DATA.respCityInfo.z;
                rqstLegionSeize.force = false;
                rqstLegionCre.sezAct = rqstLegionSeize;
            } else if (tgtElmet.type == SceneElemtTypeLegion.LEGION.getCode() || tgtElmet.type == SceneElemtTypeLegion.NPC_LEGION.getCode() || tgtElmet.type == SceneElemtTypeLegion.UNION_LEGION.getCode()) {
                RqstLegionAtkTgt atkAct = new RqstLegionAtkTgt();
                atkAct.tgtSix = tgtElmet.six;
                atkAct.curX = connect.CITY_DATA.respCityInfo.x;
                atkAct.curZ = connect.CITY_DATA.respCityInfo.z;
                rqstLegionCre.atkAct = atkAct;
            }
            if (connect.isSending(Commands.LEGION_CRE_CONST)) {
                return CompletableFuture.completedFuture(0);
            }
            return connect.send(Commands.LEGION_CRE_CONST, rqstLegionCre);
        }, connect.executor());
    }

    public static Collection<RespHero> availableHeros(MockConnect connect) {
        RespHeroInfo respHeroInfo = connect.HERO_DATA.respHeroInfo;
        return respHeroInfo.heros.values().stream().filter(it -> it.uniqIxOfHolder == 0).collect(Collectors.toList());
    }

    /**
     * 战斗||采集
     * 查找当前视野是否有单位
     * 没有的话城市视野里的单位
     * 还是没有请求搜索野怪（NPC）
     * 然后有军团就使用军团攻击
     * 没有就创建军团攻击
     * 如果没有兵力，gm生成兵力
     */

    public static void execute(MockConnect connect) {
        LegionData legion_data = connect.LEGION_DATA;
        checkCreLegion(connect, legion_data);
        for (LegionState value : legion_data.legionStates.values()) {
            legionStateSwitch(connect, value);
            legionAction(connect, value);
            if (value.legion.state instanceof RespLegionStatePathMv) {
                SceneController.rqstElemt(connect, 2, connect.ROLE_DATA.respRoleInfo.roleInfo.id, value.legion.uniqIxOfHolder);
            }
        }
        connect.send(Commands.LEGION_LOAD_CONST, null);
    }

    private static void legionAction(MockConnect connect, LegionState legionState) {

        if (!legionState.isIdle()) {
            return;
        }
        if (legionState.legion.actionType == LegionState.LEGION_ON_CALC_ING) {
            return;
        }
        RespSceneElemtSelector selector = RandomUtils.random(getSelectors());
        legionState.setCalcing();
        CompletableFuture<RespSceneElemt> future = findElemt(connect, selector, legionState.legion);
        RespSceneElemt[] respSceneElemts = new RespSceneElemt[1];
        future.thenComposeAsync(elemt -> {
            if (elemt == NULL_ELEMT) {
                legionState.setState(LegionActionType.WAIT.getType());
                return CompletableFuture.completedFuture(0);
            }
            respSceneElemts[0] = elemt;
            return fight(connect, legionState.legion, elemt);
        }, connect.executor()).exceptionally(e -> {
            legionState.setState(LegionActionType.WAIT.getType());
            int codeInEx = CodeException.getCodeInEx(e);
            if (codeInEx == CodeLegion.LEGION_DISCARD.getCode()) {
                legionState.setState(LegionActionType.RETURN.getType());
            } else if (codeInEx == CodeLegion.CANNT_FIND_LEGION.getCode()) {
                legionState.setState(LegionActionType.RETURN.getType());
                connect.send(Commands.LEGION_LOAD_CONST, null);
            } else if (codeInEx == CodeScene.CANNT_FIND_SCENE_ELEMT.getCode()) {
                retCity(connect, legionState.legion);
                legionState.setState(LegionActionType.RETURN.getType());
                //请求更新数据
                RespSceneElemt respSceneElemt = respSceneElemts[0];
                if (respSceneElemt != null) {
                    connect.SCENE_DATA.respSceneElemtMap.remove(respSceneElemt.six);
                }
            } else {
                logger.error("legionAction", e);
            }
            return null;
        });
    }

    static ArrayList<RespSceneElemtSelector> respSceneElemtSelectors = Lists.newArrayList(
            RsrcRespSceneElemtSelector.INSTANCE
            ,NpcRespSceneElemtSelector.MONSTER
    );

    private static List<RespSceneElemtSelector> getSelectors() {
        return respSceneElemtSelectors;
    }

    public static RespSceneElemt NULL_ELEMT = new RespSceneElemt();

    private static void checkCreLegion(MockConnect connect, LegionData legion_data) {
        legion_data.resetQueueSize(connect);//重置最大军团数
        if (legion_data.onCreCount >= legion_data.maxQueueSize) {//正在创建的个数超过最大军团大小
            return;
        }
        if (legion_data.legionSize() >= legion_data.maxQueueSize) {//当前军团个数超过最大军团大小
            return;
        }
        if (connect.isSending(Commands.LEGION_CRE_CONST)) {
            return;
        }
        RespSceneElemtSelector selector = RandomUtils.random(getSelectors());
        CompletableFuture<RespSceneElemt> future = findElemt(connect, selector, null);
        legion_data.onCreCount++;
        RespSceneElemt[] respSceneElemts = new RespSceneElemt[1];
        future.thenComposeAsync(elemt -> {
            if (elemt == NULL_ELEMT) {
                return CompletableFuture.completedFuture(0);
            }
            respSceneElemts[0] = elemt;
            return createLegion(connect, elemt);
        }, connect.executor()).whenCompleteAsync((result, err) -> {
            legion_data.onCreCount--;
            if (err != null) {
                int codeInEx = CodeException.getCodeInEx(err.getCause());
                if (codeInEx == CodeScene.CANNT_FIND_SCENE_ELEMT.getCode()) {
                    RespSceneElemt respSceneElemt = respSceneElemts[0];
                    if (respSceneElemt != null) {
                        connect.SCENE_DATA.respSceneElemtMap.remove(respSceneElemt.six);
                    }
                }
            }
        }, connect.executor());
    }

    public static CompletableFuture<RespSceneElemt> findElemt(MockConnect connect, RespSceneElemtSelector selector, RespLegion legion) {
        CompletableFuture<RespSceneElemt> sceneElemtFuture = new CompletableFuture<>();
        //内存中查找
        RespSceneElemt elemt = SceneController.findElemt(connect, selector);
        if (elemt != null) {
            sceneElemtFuture.complete(elemt);
            return sceneElemtFuture;
        }

        int x = connect.CITY_DATA.respCityInfo.x;
        int z = connect.CITY_DATA.respCityInfo.z;
        int lv = connect.CITY_DATA.respCityInfo.lv;
        if (selector == NpcRespSceneElemtSelector.MONSTER) {
            Iterator<RespCanAtkNpcLv> iterator = connect.CITY_DATA.respCityInfo.canAtkNpcLvMap.values().iterator();
            if (iterator.hasNext()) {
                RespCanAtkNpcLv next = iterator.next();
                lv = next.npcTypeLv.getOrDefault(NpcType.Scene.getCode(), 1);
            }
            CompletableFuture<Integer> searchFuture = NpcController.searchNpc(legion, connect, RandomUtils.nextInt(lv) + 1);
            searchFuture.thenRunAsync(() -> {
                RespNpcInfo as = connect.LAST_RECV_MSGS.get(Commands.NPC_GET_CONST).as(RespNpcInfo.class);
                if (as.legions != null) {
                    sceneElemtFuture.complete(as.legions);
                } else {
                    sceneElemtFuture.complete(NULL_ELEMT);
                }
            }, connect.executor()).exceptionally(e -> {
                sceneElemtFuture.complete(NULL_ELEMT);
                return null;
            });
        } else {
            //请求服务器获取周围数据
            CompletableFuture<Integer> send = SceneController.rqstElemt(connect, 3, x, z);
            send.thenRunAsync(() -> {
                RespSceneElemt npc1 = SceneController.findElemt(connect, selector);
                if (npc1 != null) {
                    sceneElemtFuture.complete(npc1);
                } else {
                    sceneElemtFuture.complete(NULL_ELEMT);
                }

            }, connect.executor()).exceptionally(e -> {
                sceneElemtFuture.complete(NULL_ELEMT);
                return null;
            });
        }
        return sceneElemtFuture;
    }

}
