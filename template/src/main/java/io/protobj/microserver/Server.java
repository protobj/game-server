package io.protobj.microserver;

import io.protobj.IServer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class Server implements IServer {

    protected Executor executor;//逻辑执行器

    protected ApplicationContext springContext;

    protected ServerConfig config;

    protected ThreadGroup threadGroup;

    public CompletableFuture<?> start(ServerType serverType, int serverId, Properties properties) {
        this.config = new ServerConfig(serverType, serverId, properties);
        this.threadGroup = new ThreadGroup(serverType.toFullSvrId(serverId));
        this.executor = initExecutor();
        return CompletableFuture.runAsync(() -> {
            startSpring();

        }, this.executor);
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
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Configuration.class);
        return new ArrayList<>(classes);
    }

    protected abstract Executor initExecutor();


    @Override
    public <T> T getBeanByType(Class<T> clazz) {
        return springContext.getBean(clazz);
    }


}
