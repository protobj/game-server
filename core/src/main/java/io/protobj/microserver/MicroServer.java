package io.protobj.microserver;

import io.protobj.BeanContainer;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class MicroServer implements BeanContainer {
    public static final String SERVICE_PACKAGE = "service";
    public static final String EVENT_PACKAGE = "event";
    public static final String RESOURCE_PACKAGE = "resource";
    public static final String ENTITY_PACKAGE = "entity";
    public static final String MESSAGE_PACKAGE = "message";


    private Executor executor;//逻辑执行器

    private ApplicationContext springContext;


    public CompletableFuture<?> start() {

    }



    @Override
    public <T> T getBeanByType(Class<T> clazz) {
        return springContext.getBean(clazz);
    }
}
