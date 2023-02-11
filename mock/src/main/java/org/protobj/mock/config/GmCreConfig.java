package org.protobj.mock.config;

import java.util.Map;

public class GmCreConfig extends BaseConfig {

    int creLv;

    @Override
    public void read() {
        super.read();
        this.creLv = Integer.getInteger("creLv");
    }

    public int getCreLv() {
        return creLv;
    }

    public void setCreLv(int creLv) {
        this.creLv = creLv;
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = super.toMap();
        map.put("creLv", String.valueOf(creLv));
        return map;
    }
}
