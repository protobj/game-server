package io.protobj.event;

import io.protobj.BeanContainer;
import io.protobj.Module;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface EvenBus {

    String SUBSCRIBER_PACKAGE = "service";


    void register(List<Module> moduleList, BeanContainer beanContainer);

    void register(Object bean);

    void registerSubscriber(Subscriber subscriber, Class<? extends Event> event);

    void post(Response response, EventHolder holder, Event event);

    default void post(EventHolder holder, Event event) {
        post(null, holder, event);
    }

    CompletableFuture<?> postAsync(Response response, EventHolder holder, Event event, Executor executor);

    default CompletableFuture<?> postAsync(EventHolder holder, Event event, Executor executor) {
        return postAsync(null, holder, event, executor);
    }

}
