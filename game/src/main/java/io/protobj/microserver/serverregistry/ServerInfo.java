package io.protobj.microserver.serverregistry;


import io.protobj.microserver.ServerType;
import io.protobj.microserver.SvrState;
import io.protobj.microserver.loadbalance.SelectSvrStrategy;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created on 2021/6/23.
 *
 * @author chen qiang
 */
public class ServerInfo {
    //服务器唯一id
    private int serverId;
    //服务器组id== 一种用途是8个游戏服为一个联盟服务器,
    private int groupId;
    private ServerType ServerType;
    //服务器负载率，该值由所在服务器计算，越小越好
    private int loadRate;
    //服务器状态 -1关闭:0:正常 1:维护
    private SvrState state = SvrState.Up;
    private List<Integer> slots;

    private String host;

    private int port;

    private transient String fullSvrId;
    private transient BitSet slotBits;

    public ServerInfo init() {

        fullSvrId = ServerType.toFullSvrId(serverId);
        if (slots == null) {
            slots = new ArrayList<>();
        }
        if (ServerType.getSelectSvrStrategy() == SelectSvrStrategy.ConsistentHash) {
            slotBits = new BitSet();
            for (int i = 0; i < slots.size(); i += 2) {
                slotBits.set(slots.get(i), slots.get(i + 1) + 1);
            }
        }
        return this;
    }

    public BitSet getSlotBits() {
        return slotBits;
    }

    /**
     * 获取发现该服务器的消息队列
     *
     */
    public String getTopic() {
        return String.format("non-persistent://public/default/%s_%s", ServerType.name(), serverId);
    }

    public boolean isUp() {
        return state == SvrState.Up;
    }

    public boolean isMaintenance() {
        return state == SvrState.Maintenance;
    }

    public void close() {
        this.state = SvrState.Down;
    }

    public boolean isClose() {
        return this.state == SvrState.Down;
    }


    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getLoadRate() {
        return loadRate;
    }

    public void setLoadRate(int loadRate) {
        this.loadRate = loadRate;
    }

    public void addLoadRate() {
        this.loadRate++;
    }

    public ServerType getServerType() {
        return ServerType;
    }

    public void setServerType(ServerType ServerType) {
        this.ServerType = ServerType;
    }


    public void decLoadRate() {
        this.loadRate--;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public SvrState getState() {
        return state;
    }

    public void setState(SvrState state) {
        this.state = state;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public void setSlots(List<Integer> slots) {
        this.slots = slots;

    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public String getFullSvrId() {
        return fullSvrId;
    }
}
