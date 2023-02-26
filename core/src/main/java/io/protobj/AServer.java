package io.protobj;

import io.protobj.enhance.EnhanceClassCache;
import io.protobj.event.DefaultEventBus;
import io.protobj.event.EventBus;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.redisaccessor.DefaultRedisAccessor;
import io.protobj.redisaccessor.RedisAccessor;
import io.protobj.resource.ResourceManager;
import io.protobj.scheduler.SchedulerService;
import io.protobj.thread.CustomThreadFactory;
import io.protobj.thread.ExecutorGroup;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AServer implements IServer {

    protected Configuration configuration;
    protected ThreadGroup threadGroup;
    private final EnhanceClassCache classCache;
    private Executor managerExecutor;

    private ApplicationContext springContext;

    private EventBus eventBus;

    private ResourceManager resourceManager;

    private RedisAccessor redisAccessor;

    private HotSwapManger hotSwapManger;

    private Executor logicExecutor;

    private SchedulerService schedulerService;

    private SessionManager sessionManager;

    public AServer() {
        this.classCache = new EnhanceClassCache();
    }

    public CompletableFuture<?> start() {
        this.managerExecutor = Executors.newSingleThreadExecutor(CustomThreadFactory.create(threadGroup, configuration.getName() + "-manage"));
        this.logicExecutor = new ExecutorGroup(threadGroup, "LOGIC", Runtime.getRuntime().availableProcessors() - 1);
        return CompletableFuture.runAsync(() -> {
            preStart();
            startSpring();
            Map<String, Module> moduleMap = springContext.getBeansOfType(Module.class);
            List<Module> modules = moduleMap.values().stream().toList();
            try {
                start0(modules);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            postStart();
        }, this.managerExecutor);
    }

    protected abstract void preStart();

    protected abstract void postStart();

    protected void start0(List<Module> modules) throws Exception {
        initEventBus(modules);
        initResourceManager(modules);
        initRedisAccessor(modules);
        initHotSwapManager(modules);
        initSchedulerService(modules);
        initSessionManager();
        initCustom(modules);
        initNet();
    }

    protected void initCustom(List<Module> modules) {

    }

    protected void initSessionManager() {
        this.sessionManager = new SessionManager();
    }

    protected abstract void initNet();

    protected void initSchedulerService(List<Module> modules) {
        this.schedulerService = new SchedulerService();
        schedulerService.init(modules, this);
    }

    protected void initHotSwapManager(List<Module> modules) {
        this.hotSwapManger = new HotSwapManger(configuration.getHotSwap());
        hotSwapManger.start();
    }

    protected void initRedisAccessor(List<Module> modules) throws Exception {
        this.redisAccessor = new DefaultRedisAccessor(configuration.getRedis());

        redisAccessor.init(modules);
    }

    protected void initResourceManager(List<Module> modules) {
        this.resourceManager = new ResourceManager(configuration.getResource());

        resourceManager.loadResource(modules, this, false);
    }

    protected void initEventBus(List<Module> modules) {
        this.eventBus = new DefaultEventBus();

        eventBus.register(modules, this);
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

    public CompletableFuture<?> stop() {
        schedulerService.shutDown();
        return redisAccessor.close()
                .toFuture();
    }

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

    @Override
    public SessionManager sessionManager() {
        return sessionManager;
    }
}
