package io.protobj.msgdispatcher;

import io.protobj.IServer;
import io.protobj.Module;
import io.protobj.msg.Message;
import io.protobj.network.gateway.backend.client.session.Session;
import io.protobj.thread.ExecutorGroup;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class MsgDispatcherManager implements MsgDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MsgDispatcherManager.class);

    private final Map<Class, INetHandler> netHandlerMap = new ConcurrentHashMap<>();
    private ExecutorGroup executorGroup;

    public void init(List<Module> moduleList, IServer server) {
        this.executorGroup = (ExecutorGroup) server.getLogicExecutor();
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.MethodsAnnotated);
        for (Module module : moduleList) {
            configurationBuilder.forPackages(module.getClass().getPackage().getName() + "." + IServer.SERVICE_PACKAGE);
        }
        Reflections reflections = new Reflections(configurationBuilder);
        Set<Method> annotatedWith = reflections.getMethodsAnnotatedWith(NetHandler.class);

        for (Method method : annotatedWith) {
            Class<?> declaringClass = method.getDeclaringClass();
            Object bean = server.getBeanByType(declaringClass);
            final Parameter[] parameters = method.getParameters();
            Class<?> type = parameters[0].getType();
            if (parameters.length != 1 || !type.isAssignableFrom(Message.class)) {
                logger.error(" {} 第一个参数不是 Message 子类 ", method.getName());
                continue;
            }
            final INetHandler netHandler = DefaultNetHandler.enhanceNetHandler(bean, method, type, server);
            INetHandler old = netHandlerMap.put(type, netHandler);
            if (old != null) {
                throw new IllegalArgumentException("消息[%s]有重复NetHandler监听".formatted(type.getName()));
            }
        }
    }

    @Override
    public void dispatch(Session session, Message msg) {
        INetHandler netHandler = netHandlerMap.get(msg.getClass());
        try {
            Executor executor = session.executor();
            if (executor == null) {
                session.setExecutor(executorGroup.next());
            }
            CompletableFuture<?> future = netHandler.invoke(session, msg);
            future.thenAccept(resp -> session.sendMsg(msg.index(), resp));

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
