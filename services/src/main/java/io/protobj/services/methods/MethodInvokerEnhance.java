package io.protobj.services.methods;

import io.protobj.IServer;
import io.protobj.enhance.EnhanceClassCache;
import io.protobj.services.annotations.Service;
import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;
import io.protobj.services.annotations.Slot;
import io.protobj.services.api.Message;
import javassist.*;
import javassist.Modifier;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.SignatureAttribute;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MethodInvokerEnhance {

    private static final String dir = "./clzTemp";

    public static MethodInvoker create(Object service, Class<?> methodInterface, Method method, IServer server) throws Exception {
        Service annotation = methodInterface.getAnnotation(Service.class);
        int st = annotation.st();
        Service methodAnnotation = method.getAnnotation(Service.class);
        int id = st + methodAnnotation.ix();
        Parameter[] parameters = method.getParameters();
        String className = service.getClass().getName() + "MethodInvoker" + id;
        System.err.println(id);
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
        pool.appendClassPath(dir);
        CtClass newClass = pool.makeClass(className);

        newClass.addField(CtField.make("public " + server.getClass().getName() + " bean;", newClass));

        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.get(Object.class.getName())}, newClass);
        ctConstructor.setBody("{$0.bean= (" + server.getClass().getName() + ")$1;}");
        newClass.addConstructor(ctConstructor);

        newClass.addMethod(CtMethod.make("public int cmd(){return " + id + ";}", newClass));
        CommunicationMode communicationMode = Reflect.communicationMode(method);

        newClass.addMethod(CtMethod.make("public " + CommunicationMode.class.getName() + " mode(){return " + CommunicationMode.class.getName() + "." + communicationMode.name() + ";}", newClass));
        Class<? extends Message.Content> parameterClass;
        try {
            parameterClass = getOrCreateParameterClass(service.getClass().getName() + "Message" + id, parameters, server);
            newClass.addMethod(CtMethod.make("public " + Type.class.getName() + " parameterType(){return " + (parameterClass == null ? "null" : parameterClass.getName() + ".class") + ";}", newClass));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        boolean parameterIsContent = parameters.length != 0 && (Message.Content.class.isAssignableFrom(parameters[0].getType()) || parameters[0].getType() == Flux.class);
        switch (communicationMode) {
            case FIRE_AND_FORGET: {
                StringBuilder sb = new StringBuilder();
                sb.append("public void invoke(io.protobj.services.api.Message.Content content) {\n");
                if (parameters.length == 0) {
                    sb.append("bean." + method.getName() + "();");
                } else if (parameterIsContent) {
                    sb.append("bean." + method.getName() + "((" + parameterClass.getName() + ")content);");
                } else {
                    sb.append(parameterClass.getName() + " m = (" + parameterClass.getName() + ")content;");
                    sb.append("bean." + method.getName() + "(");
                    for (int i = 0; i < parameters.length; i++) {
                        String name = parameters[i].getName();
                        sb.append("m." + name);
                        if (i != parameters.length - 1) {
                            sb.append(",");
                        }
                    }
                    sb.append(");");
                }
                sb.append("}\n");
                newClass.addMethod(CtMethod.make(sb.toString(), newClass));
                break;
            }
            case REQUEST_RESPONSE:
            case REQUEST_RESPONSE_BLOCK: {
                StringBuilder sb = new StringBuilder();
                sb.append("public reactor.core.publisher.Mono<io.protobj.services.api.Message.Content> invokeOne(io.protobj.services.api.Message.Content content) {\n");
                if (parameters.length == 0) {
                    sb.append("return bean." + method.getName() + "().cast(io.protobj.services.api.Message.Content.class);");
                } else if (parameterIsContent) {
                    sb.append("return bean." + method.getName() + "((" + parameterClass.getName() + ")content).cast(io.protobj.services.api.Message.Content.class);");
                } else {
                    sb.append(parameterClass.getName() + " m = (" + parameterClass.getName() + ")content;");
                    sb.append("return bean." + method.getName() + "(");
                    for (int i = 0; i < parameters.length; i++) {
                        String name = parameters[i].getName();
                        sb.append("m." + name);
                        if (i != parameters.length - 1) {
                            sb.append(",");
                        }
                    }
                    sb.append(").cast(io.protobj.services.api.Message.Content.class);");
                }
                sb.append("}\n");
                newClass.addMethod(CtMethod.make(sb.toString(), newClass));
                break;
            }
            case REQUEST_STREAM: {
                StringBuilder sb = new StringBuilder();
                sb.append("public reactor.core.publisher.Flux<io.protobj.services.api.Message.Content> invokeMany(io.protobj.services.api.Message.Content content) {\n");
                if (parameters.length == 0) {
                    sb.append("return bean." + method.getName() + "().cast(io.protobj.services.api.Message.Content.class);");
                } else if (parameterIsContent) {
                    sb.append("return bean." + method.getName() + "((" + parameterClass.getName() + ")content).cast(io.protobj.services.api.Message.Content.class);");
                } else {
                    sb.append(parameterClass.getName() + " m = (" + parameterClass.getName() + ")content;");
                    sb.append("return bean." + method.getName() + "(");
                    for (int i = 0; i < parameters.length; i++) {
                        String name = parameters[i].getName();
                        sb.append("m." + name);
                        if (i != parameters.length - 1) {
                            sb.append(",");
                        }
                    }
                    sb.append(").cast(io.protobj.services.api.Message.Content.class);");
                }
                sb.append("}\n");
                newClass.addMethod(CtMethod.make(sb.toString(), newClass));
                break;
            }
            case REQUEST_CHANNEL: {
                StringBuilder sb = new StringBuilder();
                sb.append("public reactor.core.publisher.Flux<io.protobj.services.api.Message.Content> invokeBidirectional(reactor.core.publisher.Flux<io.protobj.services.api.Message.Content> content) {\n");
                sb.append("return bean." + method.getName() + "(content.cast(" + parameterClass.getName() + ".class)).cast(io.protobj.services.api.Message.Content.class);");
                sb.append("}\n");
                newClass.addMethod(CtMethod.make(sb.toString(), newClass));
                break;
            }
            default:
                break;
        }


        newClass.writeFile(dir);
        Class<?> aClass = newClass.toClass();
        newClass.detach();
        server.getEnhanceClassCache().putEnhanceClass(aClass);
        return (MethodInvoker) aClass.getConstructor().newInstance(service.getClass().cast(service));

    }


    private static final String NOT_SUPPORT_PARAMETER_NAME_MSG = "must turn on \"Store information about method parameters (usable via reflection)\", see https://www.concretepage.com/java/jdk-8/java-8-reflection-access-to-parameter-names-of-method-and-constructor-with-maven-gradle-and-eclipse-using-parameters-compiler-argument";

    public static Class<? extends Message.Content> getOrCreateParameterClass(String className, Parameter[] parameters, IServer server) throws NotFoundException, CannotCompileException {
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
        pool.appendClassPath("");
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

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = parameter.getName();
            Class<?> paramType = parameters[i].getType();
            CtField ctField = new CtField(pool.get(paramType.getName()), paramName, newClass);
            ctField.setModifiers(Modifier.PUBLIC);
            if (parameter.isAnnotationPresent(Sid.class) || parameter.isAnnotationPresent(Sids.class)) {
                ctField.setModifiers(Modifier.PUBLIC | Modifier.TRANSIENT);
            }
            newClass.addField(ctField);
        }
        List<Parameter> collect = Arrays.stream(parameters).filter(it -> it.isAnnotationPresent(Sid.class)
                || it.isAnnotationPresent(Sids.class)
                || it.isAnnotationPresent(Slot.class)
        ).collect(Collectors.toList());
        if (collect.size() > 1) {
            throw new RuntimeException("@Sid|@Sids|@Slot repeat,only exists one");
        }
        if (collect.size() == 1) {
            Parameter parameter = collect.get(0);
            if (parameter.isAnnotationPresent(Sid.class)) {
                if (parameter.getType() != int.class) {
                    throw new RuntimeException("@Slot parameter must be int");
                }
                interfaces = new CtClass[]{pool.getCtClass(Message.SidContent.class.getName())};
                newClass.addMethod(CtMethod.make("public int sid(){return " + collect.get(0).getName() + ";}", newClass));
            } else if (parameter.isAnnotationPresent(Sids.class)) {
                if (parameter.getType() != int[].class) {
                    throw new RuntimeException("@Slot parameter must be int[]");
                }
                interfaces = new CtClass[]{pool.getCtClass(Message.SidsContent.class.getName())};
                newClass.addMethod(CtMethod.make("public int[] sids(){return " + collect.get(0).getName() + ";}", newClass));
            } else {
                if (parameter.getType() != long.class) {
                    throw new RuntimeException("@Slot parameter must be long");
                }
                interfaces = new CtClass[]{pool.getCtClass(Message.SlotContent.class.getName())};
                newClass.addMethod(CtMethod.make("public long slotKey(){return " + collect.get(0).getName() + ";}", newClass));
            }
        }

        newClass.setInterfaces(interfaces);


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
        Class<?> aClass = newClass.toClass();
        try {
            newClass.writeFile(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.getEnhanceClassCache().putEnhanceClass(aClass);
        return (Class<? extends Message.Content>) aClass;
    }

}
