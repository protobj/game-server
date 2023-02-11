package io.protobj.microserver;

import com.guangyu.cd003.projects.message.core.loadbalance.SelectSvrStrategy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created on 2021/6/23.
 *
 * @author chen qiang
 */
public enum SvrType {
    //游戏服
    Game(false, SelectSvrStrategy.SvrId, ""),
    //GM服务器
    Gm(false, SelectSvrStrategy.LoadMin, ""),
    //用户中心, 处理玩家登录, 对外服务器信息拉取
    User(false, SelectSvrStrategy.LoadMin, ""),
    //支付中心, 处理支付相关
    Pay(false, SelectSvrStrategy.LoadMin, ""),
    //客户端打点日志
    Logging(false, SelectSvrStrategy.LoadMin, ""),
    //日志数据中台服务，用于搜集游戏日志
    Data(false, SelectSvrStrategy.SvrId, "com.guangyu.cd003.projects.dataserver.DataServerBootstrap"),
    //联盟
    League(true, SelectSvrStrategy.RqstDns, "com.guangyu.cd003.projects.leagueserver.LeagueBootstrap"),
    //代理负载均衡
    DNS(false, SelectSvrStrategy.IdMin, "com.guangyu.cd003.projects.dnsserver.DnsBootstrap"),
    //行会业务处理
    Hunt(false, SelectSvrStrategy.ConsistentHash, "com.guangyu.cd003.projects.huntserver.HuntBootstrap"),
    //行会创建，人员变更
    HuntAccount(false, SelectSvrStrategy.IdMin, "com.guangyu.cd003.projects.huntaccountserver.HuntAccountBootstrap"),
    ;
    //游戏服上线下线时要处理缓存
    private boolean needLoadData;
    //子服务器启动全类名
    private String bootstrapClass;
    //选服策略
    private SelectSvrStrategy selectSvrStrategy;
    //关注哪些服务器上下线
    private static final EnumMap<SvrType, Set<SvrType>> follows;

    static {
        follows = new EnumMap<>(SvrType.class);
        follows.put(Game, EnumSet.of(Game, DNS, Data, Hunt, HuntAccount, League, User));
        follows.put(DNS, EnumSet.of(DNS));
        follows.put(Hunt, EnumSet.of(Data, HuntAccount));
        follows.put(HuntAccount, EnumSet.of(Hunt, Data));
        follows.put(User, EnumSet.of(Data));
        follows.put(Pay, EnumSet.of(Data));
        follows.put(League, EnumSet.of(Data));
        follows.put(Gm, EnumSet.of(User));
    }

    public static Set<SvrType> getFollows(SvrType svrType) {
        return follows.getOrDefault(svrType, EnumSet.noneOf(SvrType.class));
    }

    SvrType() {
    }

    SvrType(boolean needLoadData, SelectSvrStrategy selectSvrStrategy, String bootstrapClass) {
        this.needLoadData = needLoadData;
        this.bootstrapClass = bootstrapClass;
        this.selectSvrStrategy = selectSvrStrategy;
    }


    public static String getSplitter() {
        return "/";
    }

    public static SvrType getSvrType(String fullSvrId) {
        return SvrType.valueOf(fullSvrId.split(getSplitter())[0]);
    }

    public String toFullSvrId(int serverId) {
        return (name() + getSplitter() + serverId).intern();
    }

    public boolean isLoadData() {
        return needLoadData;
    }

    public String getBootstrapClass() {
        return bootstrapClass;
    }

    public SelectSvrStrategy getSelectSvrStrategy() {
        return selectSvrStrategy;
    }

}
