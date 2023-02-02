package io.protobj;

import io.lettuce.core.event.EventBus;
import io.protobj.enhance.EnhanceClassCache;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.network.gateway.NettyGateClient;
import io.protobj.redisaccessor.RedisAccessor;
import io.protobj.resource.ResourceManager;

import java.util.concurrent.Executor;

public interface IServer extends BeanContainer {
    public static final String SERVICE_PACKAGE = "service";
    public static final String EVENT_PACKAGE = "event";
    public static final String RESOURCE_PACKAGE = "resource";
    public static final String ENTITY_PACKAGE = "entity";
    public static final String MESSAGE_PACKAGE = "message";

    //代码增强
    EnhanceClassCache getEnhanceClassCache();

    //事件分发
    EventBus getEventBus();

    //资源管理
    ResourceManager getResourceManager();

    //数据库访问
    RedisAccessor getRedisAccessor();

    //热更新管理
    HotSwapManger getHotSwapManger();

    //网络连接
    NettyGateClient getNettyGateClient();


    Executor getExecutor();
}
