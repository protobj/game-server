package io.protobj.services.methods;

import io.protobj.IServer;
import io.protobj.enhance.EnhanceClassCache;
import io.protobj.services.ServiceContext;
import io.protobj.services.annotations.Service;
import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;
import io.protobj.services.annotations.Slot;
import io.protobj.services.api.Message;
import io.protobj.util.FileUtil;
import javassist.*;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RpcMethodEnhance {

    private static final String dir = "./clzTemp";

    public static MethodInvoker createInvoker(Object service, Class<?> methodInterface, Method method, IServer server) throws Exception {
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
        newClass.addInterface(pool.get(MethodInvoker.class.getName()));
        newClass.addField(CtField.make("public " + service.getClass().getName() + " bean;", newClass));

        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.get(Object.class.getName())}, newClass);
        ctConstructor.setBody("{$0.bean= (" + service.getClass().getName() + ")$1;}");
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
            case REQUEST_RESPONSE: {
                StringBuilder sb = new StringBuilder();

                sb.append("public reactor.core.publisher.Mono invokeOne(io.protobj.services.api.Message.Content content) {\n");
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
            case REQUEST_RESPONSE_BLOCK: {
                StringBuilder sb = new StringBuilder();

                sb.append("public io.protobj.services.api.Message.Content invokeOne(io.protobj.services.api.Message.Content content) {\n");
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
                    sb.append(");");
                }
                sb.append("}\n");
                newClass.addMethod(CtMethod.make(sb.toString(), newClass));
                break;
            }

            case REQUEST_STREAM: {
                StringBuilder sb = new StringBuilder();
                sb.append("public reactor.core.publisher.Flux invokeMany(io.protobj.services.api.Message.Content content) {\n");
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
                sb.append("public reactor.core.publisher.Flux invokeBidirectional(reactor.core.publisher.Flux content) {\n");
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
        return (MethodInvoker) aClass.getConstructor(Object.class).newInstance(service);

    }

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
        pool.appendClassPath(dir);
        try {
            Class enhanceClass = enhanceClassCache.getEnhanceClass(className);
            if (enhanceClass != null) {
                return (Class<? extends Message.Content>) enhanceClass;
            }
            Class<?> clazz = RpcMethodEnhance.class.getClassLoader().loadClass(className);
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
        List<Parameter> collect = Arrays.stream(parameters).filter(it -> it.isAnnotationPresent(Sid.class) || it.isAnnotationPresent(Sids.class) || it.isAnnotationPresent(Slot.class)).collect(Collectors.toList());
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

    public static <T> T createApi(Class<T> serviceInterface, ServiceContext serviceContext) throws Exception {
        IServer server = serviceContext.getServer();
        Service service = serviceInterface.getAnnotation(Service.class);
        int st = service.st();
        String className = serviceInterface.getName() + "MethodRequesterEnhance" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        EnhanceClassCache enhanceClassCache = server.getEnhanceClassCache();
        Class enhanceClass = enhanceClassCache.getEnhanceClass(className);
        if (enhanceClass != null) {
            return (T) enhanceClass.getConstructor(ServiceContext.class).newInstance(serviceContext);
        }

        ClassPool classPool = enhanceClassCache.getClassPool();
        classPool.appendClassPath(dir);
        CtClass newClass = classPool.makeClass(className);
        newClass.addInterface(classPool.get(serviceInterface.getName()));
        newClass.addField(CtField.make("public " + ServiceContext.class.getName() + " ctx;", newClass));

        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{classPool.get(ServiceContext.class.getName())}, newClass);
        ctConstructor.setBody("{$0.ctx=$1;}");
        newClass.addConstructor(ctConstructor);

        List<Method> methods = getAllServiceMethods(serviceInterface).stream().sorted((o1, o2) -> {
            int ix = o1.getAnnotation(Service.class).ix();
            int ix1 = o2.getAnnotation(Service.class).ix();
            return ix - ix1;
        }).collect(Collectors.toList());

        for (Method method : methods) {
            Service methodAnnotation = method.getAnnotation(Service.class);
            int id = st + methodAnnotation.ix();
            CommunicationMode communicationMode = Reflect.communicationMode(method);
            Parameter[] parameters = method.getParameters();
            Class<? extends Message.Content> parameterClass;
            try {
                parameterClass = getOrCreateParameterClass(serviceInterface.getName() + "Message" + id, parameters, server);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Type genericReturnType = method.getGenericReturnType();
            Class retClass = null;
            if (genericReturnType instanceof ParameterizedType){
                retClass = (Class) ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
            }else{
                retClass = (Class) genericReturnType;
            }
            boolean parameterIsContent = parameters.length != 0 && (Message.Content.class.isAssignableFrom(parameters[0].getType()) || parameters[0].getType() == Flux.class);
            List<CtClass> collect = Arrays.stream(parameters).map(it -> {
                try {
                    return classPool.get(it.getType().getName());
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            String methodDeclare = "public " + method.getReturnType().getName() + " " + method.getName() + "(";
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (parameters[0].getParameterizedType() instanceof ParameterizedType) {
                    methodDeclare = methodDeclare + parameter.getType().getName() + " " + parameters[0].getName();
                } else {
                    methodDeclare = methodDeclare + parameter.toString();
                }
                if (i != parameters.length - 1) {
                    methodDeclare = methodDeclare + ",";
                }
            }
            methodDeclare += ")";

            StringBuilder body = new StringBuilder();
            body.append("{");
            switch (communicationMode) {
                case FIRE_AND_FORGET: {
                    if (parameters.length == 0) {
                        body.append(String.format("ctx.fireForget(%d,%d,null);", st, id));
                    } else if (parameterIsContent) {
                        body.append(String.format("ctx.fireForget(%d,%d,%s);", st, id, parameters[0].getName()));
                    } else {
                        String collected = Arrays.stream(parameters).map(Parameter::getName).collect(Collectors.joining(","));
                        String msg = "new " + parameterClass.getName() + "(" + collected + ")";
                        body.append(String.format("ctx.fireForget(%d,%d,%s);", st, id, msg));
                    }
                    break;
                }
                case REQUEST_RESPONSE: {
                    if (parameters.length == 0) {
                        body.append(String.format(" return ctx.requestResponse(%d,%d,null,%s.class);", st, id,retClass.getName()));
                    } else if (parameterIsContent) {
                        body.append(String.format("return ctx.requestResponse(%d,%d,%s,%s.class);", st, id, parameters[0].getName(),retClass.getName()));
                    } else {
                        String collected = Arrays.stream(parameters).map(Parameter::getName).collect(Collectors.joining(","));
                        String msg = "new " + parameterClass.getName() + "(" + collected + ")";
                        body.append(String.format("return ctx.requestResponse(%d,%d,%s,%s.class);", st, id, msg,retClass.getName()));
                    }
                    break;
                }
                case REQUEST_RESPONSE_BLOCK: {
                    if (parameters.length == 0) {
                        body.append(String.format(" return (%s)ctx.requestResponse(%d,%d,null,%s.class).block();", retClass.getName(),st, id,retClass.getName()));
                    } else if (parameterIsContent) {
                        body.append(String.format("return (%s)ctx.requestResponse(%d,%d,%s,%s.class).block();",retClass.getName(), st, id, parameters[0].getName(),retClass.getName()));
                    } else {
                        String collected = Arrays.stream(parameters).map(Parameter::getName).collect(Collectors.joining(","));
                        String msg = "new " + parameterClass.getName() + "(" + collected + ")";
                        body.append(String.format("return (%s)ctx.requestResponse(%d,%d,%s,%s.class).block();",retClass.getName(), st, id, msg,retClass.getName()));
                    }
                    break;
                }
                case REQUEST_STREAM: {
                    if (parameters.length == 0) {
                        body.append(String.format(" return ctx.requestStream(%d,%d,null,%s.class);", st, id,retClass.getName()));
                    } else if (parameterIsContent) {
                        body.append(String.format("return ctx.requestStream(%d,%d,%s,%s.class);", st, id, parameters[0].getName(),retClass.getName()));
                    } else {
                        String collected = Arrays.stream(parameters).map(Parameter::getName).collect(Collectors.joining(","));
                        String msg = "new " + parameterClass.getName() + "(" + collected + ")";
                        body.append(String.format("return ctx.requestStream(%d,%d,%s,%s.class);", st, id, msg,retClass.getName()));
                    }
                    break;
                }
                case REQUEST_CHANNEL: {
                    if (parameters.length == 0) {
                        body.append(String.format(" return ctx.requestChannel(%d,%d,null,%s.class);", st, id,retClass.getName()));
                    } else if (parameterIsContent) {
                        body.append(String.format("return ctx.requestChannel(%d,%d,%s,%s.class);", st, id, parameters[0].getName(),retClass.getName()));
                    } else {
                        String collected = Arrays.stream(parameters).map(Parameter::getName).collect(Collectors.joining(","));
                        String msg = "new " + parameterClass.getName() + "(" + collected + ")";
                        body.append(String.format("return ctx.requestChannel(%d,%d,%s,%s.class);", st, id, msg,retClass.getName()));
                    }
                    break;
                }
            }
            body.append("}");
            CtMethod declaredMethod = CtMethod.make(methodDeclare + body, newClass);
            newClass.addMethod(declaredMethod);
        }
        newClass.writeFile(dir);
        newClass.detach();
        server.getEnhanceClassCache().putEnhanceClass(enhanceClass = newClass.toClass());

        return (T) enhanceClass.getConstructor(ServiceContext.class).newInstance(serviceContext);
    }

    private static <T> List<Method> getAllServiceMethods(Class<T> serviceInterface) {
        List<Method> methods = Arrays.stream(serviceInterface.getDeclaredMethods()).filter(it -> it.isAnnotationPresent(Service.class)).collect(Collectors.toList());
        Class<?>[] interfaces = serviceInterface.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            methods.addAll(getAllServiceMethods(anInterface));
        }
        return methods;
    }

    public static void deleteTempDir() {
        File file = new File(dir);
        FileUtil.deleteAll(file);
    }
}
