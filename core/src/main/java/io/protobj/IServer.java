package io.protobj;

import io.protobj.enhance.EnhanceClassCache;
import io.protobj.event.EventBus;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.msgdispatcher.MsgDispatcher;
import io.protobj.network.internal.session.Session;
import io.protobj.redisaccessor.RedisAccessor;
import io.protobj.resource.ResourceManager;
import io.protobj.scheduler.SchedulerService;

import java.util.concurrent.Executor;

public interface IServer extends BeanContainer {
    public static final String SERVICE_PACKAGE = "service";
    public static final String EVENT_PACKAGE = "event";
    public static final String RESOURCE_PACKAGE = "resource";
    public static final String ENTITY_PACKAGE = "entity";
    public static final String MESSAGE_PACKAGE = "message";

    ThreadGroup threadGroup();

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

    Executor getManageExecutor();

    //逻辑执行器
    Executor getLogicExecutor();


    SchedulerService schedulerService();

    SessionManager sessionManager();
}
