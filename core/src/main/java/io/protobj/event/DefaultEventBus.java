package io.protobj.event;

import io.protobj.BeanContainer;
import io.protobj.Module;
import javassist.*;
import javassist.bytecode.SignatureAttribute;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DefaultEventBus implements EvenBus {
    private static final Logger log = LoggerFactory.getLogger(DefaultEventBus.class);
    private final Map<Class<? extends Event>, List<Subscriber>> subscriberMap = new HashMap<>();

    @Override
    public void register(List<Module> moduleList, BeanContainer beanContainer) {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.MethodsAnnotated, Scanners.SubTypes);
        for (Module module : moduleList) {
            configurationBuilder.forPackages(module.getClass().getPackage().getName() + "." + SUBSCRIBER_PACKAGE);
        }
        Reflections reflections = new Reflections(configurationBuilder);
        //接口事件
        Set<Class<? extends Subscriber>> subscriberClasses = reflections.getSubTypesOf(Subscriber.class);
        for (Class<? extends Subscriber> subscriberClass : subscriberClasses) {
            ParameterizedType genericInterface = (ParameterizedType) subscriberClass.getGenericInterfaces()[0];
            Class<? extends Event> actualTypeArgument = (Class<? extends Event>) genericInterface.getActualTypeArguments()[0];
            Subscriber bean = beanContainer.getBeanByType(subscriberClass);
            if (bean == null) {
                throw new RuntimeException("class have no instance:%s".formatted(subscriberClass.getName()));
            }
            registerSubscriber(new Class[]{actualTypeArgument}, bean);
        }

        //方法事件
        Set<Method> subscribers = reflections.getMethodsAnnotatedWith(Subscribe.class);
        for (Method subscriber : subscribers) {
            checkSubscriber(subscriber);
            Object bean = beanContainer.getBeanByType(subscriber.getDeclaringClass());
            if (bean == null) {
                throw new RuntimeException("class have no instance:%s".formatted(subscriber.getDeclaringClass().getName()));
            }
            try {
                Subscriber enhanceSubscriber = createEnhanceSubscriber(subscriber, bean);
                Class[] annotationsByType = subscriber.getAnnotation(Subscribe.class).value();
                registerSubscriber(annotationsByType.length > 0 ? annotationsByType : new Class[]{subscriber.getParameterTypes()[2]}, enhanceSubscriber);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void register(Object bean) {
        if (bean instanceof Subscriber subscriber) {
            ParameterizedType genericInterface = (ParameterizedType) bean.getClass().getGenericInterfaces()[0];
            Class<? extends Event> actualTypeArgument = (Class<? extends Event>) genericInterface.getActualTypeArguments()[0];
            registerSubscriber(new Class[]{actualTypeArgument}, subscriber);
        }
        Set<Method> allMethods = ReflectionUtils.getAllMethods(bean.getClass(), it -> it.getAnnotation(Subscribe.class) != null);
        for (Method subscriber : allMethods) {
            checkSubscriber(subscriber);
            try {
                Subscriber enhanceSubscriber = createEnhanceSubscriber(subscriber, bean);
                Class[] annotationsByType = subscriber.getAnnotation(Subscribe.class).value();
                registerSubscriber(annotationsByType.length > 0 ? annotationsByType : new Class[]{subscriber.getParameterTypes()[2]}, enhanceSubscriber);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Subscriber createEnhanceSubscriber(Method subscriber, Object bean) throws Exception {
        var classPool = ClassPool.getDefault();
        Class<?> eventClazz = subscriber.getParameterTypes()[2];
        CtClass enhanceClazz = classPool.makeClass(Subscriber.class.getName() + bean.getClass().getSimpleName() + subscriber.getName() + System.nanoTime());
        enhanceClazz.addInterface(classPool.get(Subscriber.class.getCanonicalName()));
        enhanceClazz.setGenericSignature(new SignatureAttribute.TypeVariable("Subscriber<%s>".formatted(eventClazz.getSimpleName())).encode());
        CtField field = new CtField(classPool.get(bean.getClass().getCanonicalName()), "bean", enhanceClazz);
        field.setModifiers(Modifier.PRIVATE);
        enhanceClazz.addField(field);
        CtConstructor constructor = new CtConstructor(classPool.get(new String[]{bean.getClass().getCanonicalName()}), enhanceClazz);
        constructor.setBody("{this.bean=$1;}");
        constructor.setModifiers(Modifier.PUBLIC);
        enhanceClazz.addConstructor(constructor);
        CtClass[] parameters = classPool.get(new String[]{Response.class.getCanonicalName(), EventHolder.class.getCanonicalName(), Event.class.getCanonicalName()});
        CtMethod invokeMethod = new CtMethod(classPool.get(void.class.getCanonicalName()), "execute", parameters, enhanceClazz);
        invokeMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
        String invokeMethodBody = "{this.bean." + subscriber.getName() + "($1,$2,(" + eventClazz.getCanonicalName() + ")$3);}";// 强制类型转换，转换为具体的Event类型的类型
        invokeMethod.setBody(invokeMethodBody);
        enhanceClazz.addMethod(invokeMethod);
        enhanceClazz.detach();
        Class<?> resultClazz = enhanceClazz.toClass(Subscriber.class);
        Constructor<?> resultConstructor = resultClazz.getConstructor(bean.getClass());
        return (Subscriber) resultConstructor.newInstance(bean);
    }

    private void checkSubscriber(Method subscriber) {
        Class<?>[] parameterTypes = subscriber.getParameterTypes();
        if (parameterTypes.length != 3) {
            throw new IllegalArgumentException("class:%s method:%s must have three parameter".formatted(subscriber.getDeclaringClass().getName(), subscriber.getName()));
        }

        if (Response.class != parameterTypes[0]) {
            throw new IllegalArgumentException("class:%s method:%s first parameter must be Response but %s".formatted(subscriber.getDeclaringClass().getName(), subscriber.getName(), parameterTypes[0].getName()));
        }

        if (EventHolder.class != parameterTypes[1]) {
            throw new IllegalArgumentException("class:%s method:%s first parameter must be EventHolder but %s".formatted(subscriber.getDeclaringClass().getName(), subscriber.getName(), parameterTypes[1].getName()));
        }

        Subscribe subscriberAnnotation = subscriber.getAnnotation(Subscribe.class);
        Class<? extends Event>[] events = subscriberAnnotation.value();
        if (events.length == 0) {
            if (!Event.class.isAssignableFrom(parameterTypes[2])) {
                throw new IllegalArgumentException("class:%s method:%s first parameter must be Event subclass but %s".formatted(subscriber.getDeclaringClass().getName(), subscriber.getName(), parameterTypes[2].getName()));
            }
        } else {
            for (Class<? extends Event> event : events) {
                if (!parameterTypes[2].isAssignableFrom(event)) {
                    throw new IllegalArgumentException("class:%s method:%s event class:%s is not %s subclass ".formatted(subscriber.getDeclaringClass().getName(), subscriber.getName(), event.getName(), parameterTypes[2].getName()));
                }
            }
        }
        if (!Modifier.isPublic(subscriber.getModifiers()) || Modifier.isStatic(subscriber.getModifiers())) {
            throw new IllegalArgumentException("class:%s method:%s must public and no static".formatted(subscriber.getDeclaringClass().getName(), subscriber.getName()));
        }
    }

    private void registerSubscriber(Class<? extends Event>[] events, Subscriber subscriber) {
        for (Class<? extends Event> event : events) {
            registerSubscriber(subscriber, event);
        }
    }

    public void registerSubscriber(Subscriber subscriber, Class<? extends Event> event) {
        subscriberMap.computeIfAbsent(event, it -> new ArrayList<>()).add(subscriber);
    }

    @Override
    public void post(Response response, EventHolder holder, Event event) {
        List<Subscriber> subscribers = subscriberMap.get(event.getClass());
        if (subscribers == null) {
            return;
        }
        for (Subscriber subscriber : subscribers) {
            try {

                subscriber.execute(response, holder, event);
            } catch (Throwable throwable) {
                log.error("event execute error :%s".formatted(subscriber.getClass().getName()), throwable);
            }
        }
    }

    @Override
    public CompletableFuture<?> postAsync(Response response, EventHolder holder, Event event, Executor executor) {
        List<Subscriber> subscribers = subscriberMap.get(event.getClass());
        if (subscribers == null || subscribers.size() == 0) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] futures = new CompletableFuture<?>[subscribers.size()];
        int i = 0;
        for (Subscriber subscriber : subscribers) {
            futures[i++] = (CompletableFuture.runAsync(() -> subscriber.execute(response, holder, event), executor).exceptionally(e -> {
                log.error("event execute error :%s".formatted(subscriber.getClass().getName()), e);
                return null;
            }));
        }
        return CompletableFuture.allOf(futures);
    }

}
