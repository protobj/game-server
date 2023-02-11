package io.protobj.microserver.loadbalance;

import io.protobj.microserver.serverregistry.ServerInfo;
import org.apache.commons.lang3.RandomUtils;

import java.util.Comparator;
import java.util.List;

public enum SelectSvrStrategy {
    //选择服务器id最小的一台,这种模式只能支持（主备）
    // 一台主服务器，一台从服务器，当主服务器宕机后，从服务器会被选择
    //启动两台时，id小的先启动，有服务器下线时，新启动的服务器id必须大于在线的服务器，避免其他服务器同时选到两台
    IdMin {
        @Override
        public ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos) {
            serverInfos.sort(Comparator.comparingInt(ServerInfo::getServerId));
            return serverInfos.get(0);
        }
    },
    //选择跟请求服务器id相同的一台
    EqGsId {
        @Override
        public ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos) {
            for (ServerInfo serverInfo : serverInfos) {
                if (selfInfo.getServerId() == serverInfo.getServerId()) {
                    return serverInfo;
                }
            }
            throw new RuntimeException("EqGsId策略没有选出结果");
        }
    },
    //重定向到dns服务器选择
    RqstDns {
        @Override
        public ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos) {
            throw new UnsupportedOperationException("由dns服务器选择");
        }
    },
    //负载最小
    LoadMin {
        @Override
        public ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos) {
            serverInfos.sort(Comparator.comparingInt(ServerInfo::getLoadRate));
            return serverInfos.get(0);
        }
    },
    //一致性hash,随机选一个，由producer扩展如何发送
    ConsistentHash {
        @Override
        public ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos) {
            return serverInfos.get(RandomUtils.nextInt(0, serverInfos.size()));
        }
    },
    //只能通过id访问
    SvrId {
        @Override
        public ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos) {
            return null;
        }
    };

    public abstract ServerInfo select(ServerInfo selfInfo, List<ServerInfo> serverInfos);
}
