package io.protobj.event;

import io.protobj.IServer;
import io.protobj.Module;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface EventBus {




    void register(List<Module> moduleList, IServer server);

    void register(Object bean, IServer server);

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
