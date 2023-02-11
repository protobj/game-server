package io.protobj.microserver;


import java.util.Properties;

public class Configuration extends io.protobj.Configuration {

    private volatile ServerType svrType;//服务类型
    private volatile int id;//服务id
    private volatile Properties properties;//服务器配置

    public Configuration(ServerType svrType, int id, Properties properties) {
        this.svrType = svrType;
        this.id = id;
        this.properties = properties;
    }

    public ServerType getSvrType() {
        return svrType;
    }

    public void setSvrType(ServerType svrType) {
        this.svrType = svrType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
