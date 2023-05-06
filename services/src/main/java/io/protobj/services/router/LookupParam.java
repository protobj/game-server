package io.protobj.services.router;

public class LookupParam {


    public static final int SID = 0x01;//根据sid指定访问 param:sid
    public static final int DNS = 0x10;//集群内部选择一台服务器
    public static final int MIN = 0x100;//选择id最小的服务器访问
    public static final int HASH = 0x1000;//一致性hash
    private final int type;
    private final int param;

    public LookupParam(int type, int param) {
        this.type = type;
        this.param = param;
    }

    public LookupParam(int type) {
        this.type = type;
        this.param = 0;
    }

    public static LookupParam sid(int param) {
        return new LookupParam(SID, param);
    }

    public static LookupParam dns(int param) {
        return new LookupParam(DNS, param);
    }

    public static LookupParam min(int param) {
        return new LookupParam(MIN, param);
    }

    public static LookupParam hash() {
        return new LookupParam(HASH);
    }

    public int getType() {
        return type;
    }

    public int getParam() {
        return param;
    }
}
