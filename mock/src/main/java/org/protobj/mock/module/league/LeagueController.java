package org.protobj.mock.module.league;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.common.module.language.cfg.LeagueLanguageSettingCfg;
import com.guangyu.cd003.projects.common.module.league.cfg.LeagueCfg;
import com.guangyu.cd003.projects.common.module.league.msg.*;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.util.RandomUtils;
import io.netty.util.collection.IntObjectMap;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LeagueController {
    public static void joinOrCre(MockConnect connect) {
        switch (connect.LEAGUE_DATA.joinOrCreState) {
            case InitCheck: {
                initCheck(connect);
                break;
            }
            case tryJoin: {
                notJoin(connect);
                break;
            }
            case Joined: {
                joined(connect);
                break;
            }
            case tryCre: {
                tryCre(connect);
                break;
            }
            case InfoGeted: {
                infoGeted(connect);
                break;
            }
            default:
                break;
        }

    }

    private static void infoGeted(MockConnect connect) {
        //领取联盟资源
        connect.send(Commands.LEAGUE_BUILDING_DRAW_RSRC_REWARD_CONST, null);

    }

    private static void tryCre(MockConnect connect) {
        connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.tryCreRqsting);
        RqstCreateLeague rqstCreateLeague = new RqstCreateLeague();
        rqstCreateLeague.settings = new HashMap<>();
        rqstCreateLeague.settings.put(LeagueSetting.AUTO_JOIN.name(), 1L);
        IntObjectMap<LeagueCfg> cfgsByClz = CacheConfig.getCfgsByClz(LeagueCfg.class);
        LeagueCfg leagueCfg = cfgsByClz.values().iterator().next();
        int[] nameLengthLimit = leagueCfg.getNameLengthLimit();
        rqstCreateLeague.leagueName = getName(nameLengthLimit);
        rqstCreateLeague.leagueNickName = getName(leagueCfg.getNickNameLengthLimit());
        rqstCreateLeague.language = CacheConfig.getCfgsByClz(LeagueLanguageSettingCfg.class).values().iterator().next().getId();
        rqstCreateLeague.notice = getName(new int[]{4, leagueCfg.getNoticeLengthLimit()});
        rqstCreateLeague.icon = "7_8_20_2";
        connect.send(Commands.LEAGUE_CRE_CONST, rqstCreateLeague).whenCompleteAsync((r, e) -> {
            if (e != null) {
                connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.InitCheck);
            } else {

                connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.Joined);
            }
        }, connect.executor());
    }

    private static String getName(int[] limits) {
        StringBuilder sb = new StringBuilder();
        int anInt = RandomUtils.nextInt(limits);
        for (int i = 0; i < anInt; i++) {
            sb.append(names[RandomUtils.nextInt(names.length)]);
        }
        return sb.toString();
    }

    public static char[] names = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static void joined(MockConnect connect) {
        connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.GetInfoRqsting);
        RqstLeagueInfo rqstLeagueInfo = new RqstLeagueInfo();
        rqstLeagueInfo.leagueId = connect.ROLE_DATA.respRoleInfo.roleInfo.leagueId;
        connect.send(Commands.LEAGUE_INFO_CONST, rqstLeagueInfo).whenCompleteAsync((integer, throwable) -> {
            if (throwable != null) {
                connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.InitCheck);
            } else {
                connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.InfoGeted);
            }
        }, connect.executor());
    }

    private static void notJoin(MockConnect connect) {
        connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.tryJoinRqsting);
        //先搜索
        RqstLeagueSearch rqstLeagueSearch = new RqstLeagueSearch();
        connect.send(Commands.LEAGUE_SEARCH_CONST, rqstLeagueSearch).thenRunAsync(() -> {
            RespLeagueSearchInfo respLeagueSearchInfo = connect.LEAGUE_DATA.respLeagueOp.respLeagueSearchInfo;
            if (CollectionUtils.isNotEmpty(respLeagueSearchInfo.appliedLeagues)) {
                RqstApplyJoinLeague rqstApplyJoinLeague = new RqstApplyJoinLeague();
                rqstApplyJoinLeague.leagueId = respLeagueSearchInfo.appliedLeagues.get(0);
                rqstApplyJoinLeague.cancel = true;
                connect.send(Commands.LEAGUE_JOIN_CONST, rqstApplyJoinLeague).whenCompleteAsync((r, e) -> {
                    if (e != null) {
                        connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.InitCheck);
                    } else {
                        connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.tryJoin);
                    }
                }, connect.executor());
            } else if (CollectionUtils.isEmpty(respLeagueSearchInfo.searchList)) {
                connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.tryCre);
            } else {
                List<RespLeagueSearch> searchList = respLeagueSearchInfo.searchList
                        .stream().filter(it -> it.count < it.limitCount / 5)
                        .filter(it -> it.settings.getOrDefault(LeagueSetting.AUTO_JOIN.name(), 0L) == 1)
                        .filter(it -> it.disbandTime == 0)
                        .collect(Collectors.toList());
                if (searchList.isEmpty()) {
                    connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.tryCre);
                } else {
                    RespLeagueSearch search = RandomUtils.random(searchList);
                    RqstApplyJoinLeague rqstApplyJoinLeague = new RqstApplyJoinLeague();
                    rqstApplyJoinLeague.leagueId = search.leagueId;
                    connect.send(Commands.LEAGUE_JOIN_CONST, rqstApplyJoinLeague).whenCompleteAsync((r, e) -> {
                        if (e != null) {
                            connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.InitCheck);
                        } else {
                            connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.Joined);
                        }
                    }, connect.executor());
                }
            }
        }, connect.executor());
    }

    private static void initCheck(MockConnect connect) {
        boolean have = connect.ROLE_DATA.respRoleInfo.roleInfo.leagueId > 0;
        if (have) {
            connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.Joined);
        } else {
            connect.LEAGUE_DATA.setJoinOrCreState(LeagueJoinOrCreState.tryJoin);
        }
    }
}
