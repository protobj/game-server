package io.protobj;

import io.protobj.hotswap.HotSwapConfig;
import io.protobj.redisaccessor.config.RedisConfig;
import io.protobj.resource.ResourceConfig;

public class Configuration {

    //热更新配置
    private HotSwapConfig hotSwap;
    //redis配置
    private RedisConfig redisConfig;
    //资源配置
    private ResourceConfig resourceConfig;


    private String name;

    public HotSwapConfig getHotSwap() {
        return hotSwap;
    }

    public void setHotSwap(HotSwapConfig hotSwap) {
        this.hotSwap = hotSwap;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    public void setResourceConfig(ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
    }

    public String getName() {
        return name;
    }
}
