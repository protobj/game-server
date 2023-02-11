package io.protobj;

import io.protobj.hotswap.HotSwapConfig;
import io.protobj.redisaccessor.config.RedisConfig;
import io.protobj.resource.ResourceConfig;

public class Configuration {

    //热更新配置
    private HotSwapConfig hotSwap;
    //redis配置
    private RedisConfig redis;
    //资源配置
    private ResourceConfig resource;

    private String name;

    public HotSwapConfig getHotSwap() {
        return hotSwap;
    }

    public void setHotSwap(HotSwapConfig hotSwap) {
        this.hotSwap = hotSwap;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public ResourceConfig getResource() {
        return resource;
    }

    public void setResource(ResourceConfig resource) {
        this.resource = resource;
    }

    public String getName() {
        return name;
    }
}
