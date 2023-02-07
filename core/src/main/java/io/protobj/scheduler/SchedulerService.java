package io.protobj.scheduler;

import io.protobj.IServer;
import io.protobj.Module;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SchedulerService {

    private HashedWheelTimer timer;

    public void init(List<Module> moduleList, IServer server) {
        timer = new HashedWheelTimer(server.threadGroup(), 1000, 60, null);
        initModule(moduleList, server);
    }

    public void initModule(List<Module> moduleList, IServer server) {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.MethodsAnnotated);
        for (Module module : moduleList) {
            configurationBuilder.forPackages(module.getClass().getPackage().getName() + "." + IServer.SERVICE_PACKAGE);
        }
        Reflections reflections = new Reflections(configurationBuilder);

        Set<Method> scheduledMethods = reflections.getMethodsAnnotatedWith(Scheduled.class);
        for (Method scheduledMethod : scheduledMethods) {
            Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
            checkScheduled(scheduledMethod, scheduled);
            Object bean = server.getBeanByType(scheduledMethod.getDeclaringClass());
            if (bean == null) {
                throw new RuntimeException("class have no instance:%s".formatted(scheduledMethod.getDeclaringClass().getName()));
            }
            try {
                Scheduler scheduler = createEnhanceScheduled(scheduledMethod, bean, server, scheduled);
                if (StringUtils.isNotEmpty(scheduled.cron())) {
                    timer.cron(server.getManageExecutor(), scheduled.cron(), scheduler::invoke);
                } else if (scheduled.fixedDelay() > 0) {
                    timer.fixedDelay(server.getManageExecutor(), TimeUnit.MILLISECONDS.convert(scheduled.fixedDelay(), scheduled.timeUnit()), scheduler::invoke);
                } else {

      timer.fixedRate(server.getManageExecutor(), TimeUnit.MILLISECONDS.convert(scheduled.fixedRate(), scheduled.timeUnit()), scheduler::invoke);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Scheduler createEnhanceScheduled(Method scheduledMethod, Object bean, IServer server, Scheduled scheduled) throws Exception {
        var classPool = server.getEnhanceClassCache().getClassPool();
        String classname = Scheduler.class.getName() + bean.getClass().getSimpleName() + "$" + scheduledMethod.getName();
        Class existsClass = server.getEnhanceClassCache().getEnhanceClass(classname);
        if (existsClass != null) {
            return (Scheduler) existsClass.getConstructor(bean.getClass()).newInstance(bean);
        }
        CtClass enhanceClazz = classPool.makeClass(classname);
        enhanceClazz.addInterface(classPool.get(Scheduler.class.getCanonicalName()));
        CtField field = new CtField(classPool.get(bean.getClass().getCanonicalName()), "bean", enhanceClazz);
        field.setModifiers(Modifier.PRIVATE);
        enhanceClazz.addField(field);
        CtConstructor constructor = new CtConstructor(classPool.get(new String[]{bean.getClass().getCanonicalName()}), enhanceClazz);
        constructor.setBody("{this.bean=$1;}");
        constructor.setModifiers(Modifier.PUBLIC);
        enhanceClazz.addConstructor(constructor);
        CtMethod invokeMethod = new CtMethod(classPool.get(void.class.getCanonicalName()), "invoke", new CtClass[]{}, enhanceClazz);
        invokeMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
        String invokeMethodBody = "{this.bean." + scheduledMethod.getName() + "();}";
        invokeMethod.setBody(invokeMethodBody);
        enhanceClazz.addMethod(invokeMethod);
        enhanceClazz.detach();
        Class<?> resultClazz = enhanceClazz.toClass(Scheduler.class);
        server.getEnhanceClassCache().putEnhanceClass(resultClazz);
        return (Scheduler) resultClazz.getConstructor(bean.getClass()).newInstance(bean);
    }

    private void checkScheduled(Method scheduledMethod, Scheduled scheduled) {
        Class<?>[] parameterTypes = scheduledMethod.getParameterTypes();
        String className = scheduledMethod.getDeclaringClass().getName();
        if (parameterTypes.length != 0) {

            throw new IllegalArgumentException("class:%s method:%s must have parameter".formatted(className, scheduledMethod.getName()));
        }

        String cron = scheduled.cron();
        long fixedDelay = scheduled.fixedDelay();
        long fixedRate = scheduled.fixedRate();
        TimeUnit timeUnit = scheduled.timeUnit();
        if (StringUtils.isEmpty(cron) && fixedDelay <= 0 && fixedRate <= 0) {
            throw new IllegalArgumentException("%s.%s() Scheduled未配置参数".formatted(className, scheduledMethod.getName()));
        }
        if (StringUtils.isNotEmpty(cron) && !CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("%s.%s() cron表达式错误:%s".formatted(className, scheduledMethod.getName(), cron));
        }

        if (fixedRate > 0 && TimeUnit.MILLISECONDS.convert(fixedRate, timeUnit) < timer.getTick()) {
            throw new IllegalArgumentException("%s.%s() fixedRate值小于tick:%d".formatted(className, scheduledMethod.getName(), fixedRate));
        }
        if (fixedDelay == 0) {
            throw new IllegalArgumentException("%s.%s() fixedDelay值等于0".formatted(className, scheduledMethod.getName()));
        }
    }


}
