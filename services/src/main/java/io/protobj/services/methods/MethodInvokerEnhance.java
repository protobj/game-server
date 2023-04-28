package io.protobj.services.methods;

import io.protobj.IServer;
import io.protobj.enhance.EnhanceClassCache;
import io.protobj.services.annotations.Service;
import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;
import io.protobj.services.annotations.Slot;
import io.protobj.services.api.Message;
import javassist.*;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MethodInvokerEnhance {


    public static MethodInvoker create(Object service, Method method, IServer server) throws CannotCompileException, NotFoundException {
        Service annotation = service.getClass().getAnnotation(Service.class);
        int st = annotation.st();
        Service methodAnnotation = method.getAnnotation(Service.class);
        int id = st + methodAnnotation.ix();
        Parameter[] parameters = method.getParameters();
        String className = server.getClass().getName() + "MethodInvoker" + id;

        EnhanceClassCache enhanceClassCache = server.getEnhanceClassCache();
        Class enhanceClass = enhanceClassCache.getEnhanceClass(className);
        if (enhanceClass != null) {
            try {
                return (MethodInvoker) enhanceClass.getConstructor().newInstance(service.getClass().cast(service));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        ClassPool pool = enhanceClassCache.getClassPool();
        CtClass newClass = pool.makeClass(className);

        newClass.addField(CtField.make("public " + server.getClass().getName() + " bean;", newClass));

        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.get(Object.class.getName())}, newClass);
        ctConstructor.setBody("$0.bean= (" + server.getClass().getName() + ")$1");
        newClass.addConstructor(ctConstructor);

        newClass.addMethod(CtMethod.make("public int cmd(){return " + id + ";}", newClass));
        CommunicationMode communicationMode = Reflect.communicationMode(method);

        newClass.addMethod(CtMethod.make("public " + CommunicationMode.class.getName() + " mode(){return " + CommunicationMode.class.getName() + "." + communicationMode.name() + ";}", newClass));
        Class<? extends Message.Content> parameterClass;
        try {
            if (communicationMode != CommunicationMode.REQUEST_CHANNEL) {
                parameterClass = createParameterClass(service.getClass().getName() + "Message" + id, parameters, server);
            } else {
                parameterClass =
            }
            newClass.addMethod(CtMethod.make("public " + Type.class.getName() + " parameterType(){return " + (parameterClass == null ? "null" : parameterClass.getName() + ".class") + ";", newClass));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } switch (communicationMode) {
            case FIRE_AND_FORGET: {

                break;
            }
            case REQUEST_RESPONSE:
            case REQUEST_RESPONSE_BLOCK: {
                break;
            }
            case REQUEST_STREAM: {
                break;
            }
            case REQUEST_CHANNEL: {
                break;
            }
            default:
                break;
        }


    }


    private static final String NOT_SUPPORT_PARAMETER_NAME_MSG = "must turn on \"Store information about method parameters (usable via reflection)\", see https://www.concretepage.com/java/jdk-8/java-8-reflection-access-to-parameter-names-of-method-and-constructor-with-maven-gradle-and-eclipse-using-parameters-compiler-argument";

    public static Class<? extends Message.Content> createParameterClass(String className, Parameter[] parameters, IServer server) throws NotFoundException, CannotCompileException {
        if (parameters.length == 0) {
            return null;
        }
        if (Message.Content.class.isAssignableFrom(parameters[0].getType())) {
            return (Class<? extends Message.Content>) parameters[0].getType();
        }
        if (Flux.class == parameters[0].getType()) {
            ParameterizedType type = (ParameterizedType) parameters[0].getParameterizedType();
            return (Class<? extends Message.Content>) type.getActualTypeArguments()[0];
        }
        EnhanceClassCache enhanceClassCache = server.getEnhanceClassCache();
        ClassPool pool = enhanceClassCache.getClassPool();
        if (!parameters[0].isNamePresent()) {
            throw new RuntimeException(NOT_SUPPORT_PARAMETER_NAME_MSG);
        }
        try {
            Class enhanceClass = enhanceClassCache.getEnhanceClass(className);
            if (enhanceClass != null) {
                return (Class<? extends Message.Content>) enhanceClass;
            }
            Class<?> clazz = MethodInvokerEnhance.class.getClassLoader().loadClass(className);
            if (clazz != null) {
                return (Class<? extends Message.Content>) clazz;
            }
        } catch (ClassNotFoundException e) {
        }

        // 创建类
        CtClass newClass = pool.makeClass(className);

        CtClass[] interfaces = {pool.getCtClass(Message.Content.class.getName())};
        List<Parameter> collect = Arrays.stream(parameters).filter(it -> it.isAnnotationPresent(Sid.class)
                || it.isAnnotationPresent(Sids.class)
                || it.isAnnotationPresent(Slot.class)
        ).collect(Collectors.toList());
        if (collect.size() > 1) {
            throw new RuntimeException("@Sid|@Sids|@Slot repeat,only exists one");
        }
        if (collect.size() == 1) {
            if (collect.)
            interfaces = new CtClass[]{pool.getCtClass(Message.SidContent.class.getName())};
            newClass.addMethod(CtMethod.make("public int sid(){return " + collect.get(0).getName() + ";}", newClass));
        }
        collect = Arrays.stream(parameters).filter(it -> it.isAnnotationPresent(Sids.class)).collect(Collectors.toList());
        if (collect.size() > 1) {
            throw new RuntimeException("@Sids repeat");
        }
        if (collect.size() == 1) {
            interfaces = new CtClass[]{pool.getCtClass(Message.SidContent.class.getName())};
            newClass.addMethod(CtMethod.make("public int sid(){return " + collect.get(0).getName() + ";}", newClass));
        }

        newClass.setInterfaces(interfaces);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = parameter.getName();
            Class<?> paramType = parameters[i].getType();
            CtField ctField = new CtField(pool.get(paramType.getName()), paramName, newClass);
            ctField.setModifiers(Modifier.PUBLIC);
            if (parameter.isAnnotationPresent(Sid.class) || parameter.isAnnotationPresent(Sids.class)) {
                ctField.setModifiers(Modifier.TRANSIENT);
            }
            newClass.addField(ctField);
        }

        // 添加无参的构造函数
        CtConstructor constructor0 = new CtConstructor(null, newClass);
        constructor0.setModifiers(Modifier.PUBLIC);
        constructor0.setBody("{}");
        newClass.addConstructor(constructor0);

        // 添加有参的构造函数
        CtClass[] paramCtClassArray = new CtClass[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            CtClass paramCtClass = pool.get(paramType.getName());
            paramCtClassArray[i] = paramCtClass;
        }

        StringBuilder bodyBuilder = new StringBuilder();

        bodyBuilder.append("{\r\n");

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            bodyBuilder.append("$0.");
            bodyBuilder.append(paramName);
            bodyBuilder.append(" = $");
            bodyBuilder.append(i + 1);
            bodyBuilder.append(";\r\n");
        }
        bodyBuilder.append("}");
        CtConstructor constructor1 = new CtConstructor(paramCtClassArray, newClass);
        constructor1.setBody(bodyBuilder.toString());
        newClass.addConstructor(constructor1);
        newClass.detach();
        Class<?> aClass = newClass.toClass();
        server.getEnhanceClassCache().putEnhanceClass(aClass);
        return (Class<? extends Message.Content>) aClass;
    }

}
