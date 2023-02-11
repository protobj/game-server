package io.protobj.microserver;

public enum ServerType {

    ;


    public static String getSplitter() {
        return "/";
    }

    public static ServerType getSvrType(String fullSvrId) {
        return ServerType.valueOf(fullSvrId.split(getSplitter())[0]);
    }

    public String toFullSvrId(int serverId) {
        return (name() + getSplitter() + serverId).intern();
    }
}
