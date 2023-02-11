package io.protobj.mock.config;

import io.protobj.mock.MockApplication;
import io.protobj.mock.plan.LoadOrCrePlan;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class BaseConfig {

    String serverId;

    String[] gateway;

    String prefix;
    int turn;
    int count;
    int turnInterval;

    int printMsgLog;

    public void read() {
        this.serverId = System.getProperty("serverId");
        this.gateway = System.getProperty("gateway").split(",");
        this.prefix = System.getProperty("prefix");
        this.turn = Integer.getInteger("turn");
        this.count = Integer.getInteger("count");
        this.turnInterval = Integer.getInteger("turnInterval");
        this.printMsgLog = Integer.getInteger("printMsgLog");
    }

    public static BaseConfig valueOf(MockApplication.MockType type, Map<String, String> params) {
        String configFile = "base.properties";
        loadConfig(configFile);
        BaseConfig baseConfig;
        if (type == MockApplication.MockType.gm_cre || type == MockApplication.MockType.random) {
            configFile = "gmcre.properties";
            loadConfig(configFile);
            baseConfig = new GmCreConfig();
        } else {
            baseConfig = new BaseConfig();
        }
        if (params != null) {
            System.getProperties().putAll(params);
        }
        baseConfig.read();
        return baseConfig;
    }

    protected static void loadConfig(String configFile) {
        InputStream resourceAsStream = LoadOrCrePlan.class.getClassLoader().getResourceAsStream(configFile);
        try {
            System.getProperties().load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getTurnInterval() {
        return turnInterval;
    }

    public void setTurnInterval(int turnInterval) {
        this.turnInterval = turnInterval;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String[] getGateway() {
        return gateway;
    }

    public void setGateway(String[] gateway) {
        this.gateway = gateway;
    }


    public int getPrintMsgLog() {
        return printMsgLog;
    }

    public void setPrintMsgLog(int printMsgLog) {
        this.printMsgLog = printMsgLog;
    }

    public boolean isPrintLog(int cmd) {
//        return true;
        if (printMsgLog == -1) {
            return false;
        }
        if (printMsgLog == 0) {
            return true;
        }
        return cmd / 100 == printMsgLog;
    }

    public Map<String, String> toMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("serverId", this.serverId);
        map.put("gateway", String.join(",", this.gateway));
        map.put("prefix", prefix);
        map.put("turn", String.valueOf(turn));
        map.put("count", String.valueOf(count));
        map.put("turnInterval", String.valueOf(turnInterval));
        map.put("printMsgLog", String.valueOf(printMsgLog));
        return map;
    }
}
