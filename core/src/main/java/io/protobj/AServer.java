package io.protobj;

import io.protobj.enhance.EnhanceClassCache;
import io.protobj.event.DefaultEventBus;
import io.protobj.event.EventBus;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.network.gateway.NettyGateClient;
import io.protobj.redisaccessor.DefaultRedisAccessor;
import io.protobj.redisaccessor.RedisAccessor;
import io.protobj.resource.ResourceManager;
import io.protobj.scheduler.SchedulerService;
import io.protobj.thread.CustomThreadFactory;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AServer implements IServer {

    protected ApplicationContext springContext;

    protected Configuration configuration;

    protected ThreadGroup threadGroup;

    private EnhanceClassCache classCache;

    private EventBus eventBus;

    private ResourceManager resourceManager;

    private RedisAccessor redisAccessor;

    private HotSwapManger hotSwapManger;

    private Executor managerExecutor;
    private Executor logicExecutor;

    private SchedulerService schedulerService;

    private NettyGateClient nettyGateClient;

    public AServer(Configuration configuration, ThreadGroup threadGroup) {
        this.configuration = configuration;
        this.threadGroup = threadGroup;
        this.classCache = new EnhanceClassCache();
        this.eventBus = new DefaultEventBus();
        this.resourceManager = new ResourceManager(configuration.getResourceConfig());
        this.redisAccessor = new DefaultRedisAccessor();
        this.hotSwapManger = new HotSwapManger(configuration.getHotSwap());
        this.managerExecutor = Executors.newSingleThreadExecutor(CustomThreadFactory.create(threadGroup, configuration.getName() + "-manage"));

    }

    public CompletableFuture<?> init() {
        return CompletableFuture.runAsync(() -> {
            startSpring();
            start0();
        }, this.managerExecutor);
    }

    protected void start0() {

    }

    protected void startSpring() {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(moduleClass().toArray(new Class[0]));
        annotationConfigApplicationContext.refresh();
        springContext = annotationConfigApplicationContext;
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
        ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) springContext.getAutowireCapableBeanFactory();
        configurableListableBeanFactory.registerSingleton(this.getClass().getSimpleName(), this);
    }

    protected List<Class> moduleClass() {
        String s = getClass().getPackage().toString();
        s = s.substring(8);
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.TypesAnnotated);
        configurationBuilder.forPackages(s + ".");
        Reflections reflections = new Reflections(configurationBuilder);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(org.springframework.context.annotation.Configuration.class);
        return new ArrayList<>(classes);
    }

    protected abstract Executor initExecutor();


    @Override
    public <T> T getBeanByType(Class<T> clazz) {
        return springContext.getBean(clazz);
    }

    @Override
    public ThreadGroup threadGroup() {
        return threadGroup;
    }

    @Override
    public EnhanceClassCache getEnhanceClassCache() {
        return classCache;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    @Override
    public RedisAccessor getRedisAccessor() {
        return redisAccessor;
    }

    @Override
    public HotSwapManger getHotSwapManger() {
        return hotSwapManger;
    }

    @Override
    public NettyGateClient getNettyGateClient() {
        return nettyGateClient;
    }

    @Override
    public Executor getManageExecutor() {
        return managerExecutor;
    }

    @Override
    public Executor getLogicExecutor() {
        return logicExecutor;
    }

    @Override
    public SchedulerService schedulerService() {
        return schedulerService;
    }
}
