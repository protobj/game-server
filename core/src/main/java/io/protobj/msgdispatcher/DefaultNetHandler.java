package io.protobj.msgdispatcher;

import io.protobj.IServer;
import io.protobj.msg.Message;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class DefaultNetHandler implements INetHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultNetHandler.class);
    protected Object source;
    protected Method method;

    public DefaultNetHandler(Object source, Method method) {
        this.source = source;
        this.method = method;
    }

    @NetHandler
    public CompletableFuture<?> invoke(Message message) throws Throwable {
        return (CompletableFuture<?>) method.invoke(source, message);
    }

    public Method getMethod() {
        return method;
    }

    public static INetHandler enhanceNetHandler(Object object, Method method, Class receiveClass, IServer server) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            final ClassPool classPool = server.getEnhanceClassCache().getClassPool();
            final String classname = object.getClass().getTypeName() + "_" + receiveClass.getSimpleName() + DefaultNetHandler.class.getSimpleName();
            Class oldClass = server.getEnhanceClassCache().getEnhanceClass(classname);
            if (oldClass != null) {
                return (INetHandler) oldClass.getConstructor(object.getClass()).newInstance(object);
            }

            final CtClass ctClass = classPool.makeClass(classname);
            ctClass.setSuperclass(classPool.get(DefaultNetHandler.class.getName()));
            ctClass.addField(CtField.make("private " + object.getClass().getName() + " bean;", ctClass));
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{classPool.get(object.getClass().getName())}, ctClass);
            ctConstructor.setBody("{this.bean=$1;}");
            ctClass.addConstructor(ctConstructor);
            stringBuilder.append("\npublic java.util.concurrent.CompletableFuture invoke(io.protobj.msg.Message data) throws Throwable {\n");
            stringBuilder.append("\treturn bean." + method.getName() + "((" + receiveClass.getTypeName() + ")$1);\n");//
            stringBuilder.append("}\n");
            ctClass.addMethod(CtMethod.make(stringBuilder.toString(), ctClass));
            ctClass.detach();
            final Class<?> aClass = ctClass.toClass();
            server.getEnhanceClassCache().putEnhanceClass(aClass);
            return (INetHandler) aClass.getConstructor(object.getClass())
                    .newInstance(object);
        } catch (Exception e) {
            logger.error("创建 NetHandler 失败 \n" + stringBuilder, e);
        }
        return null;
    }
}