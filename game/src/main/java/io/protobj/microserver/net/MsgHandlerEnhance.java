package io.protobj.microserver.net;

import com.guangyu.cd003.projects.microserver.loader.MicroServerModuleLoader;
import com.pv.common.utilities.common.BeanFactory;
import com.pv.framework.gs.core.module.ModuleLoader;
import com.pv.framework.gs.core.module.config.Module;
import com.pv.framework.gs.core.module.msgproc.asm.TheUnSafe;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.*;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created on 2021/7/7.
 *
 * @author chen qiang
 */
public class MsgHandlerEnhance {
    public static Pair<Map<String, MsgHandler>, List<LoadDataBean>> initHandlers(ApplicationContext beanContext, MicroServerModuleLoader microServerModuleLoader) {
        Map<Integer, Module<?>> moduleMap = ModuleLoader.moduleMap;
        if (microServerModuleLoader != null) {
            moduleMap = microServerModuleLoader.moduleMap;
        }

        List<LoadDataBean> loadDataBeans = new ArrayList<>();
        Map<String, MsgHandler> classMsgHandlerHashMap = new HashMap<>();
        for (Module<?> value : moduleMap.values()) {
            Class<?> boBeanClass = value.getModuleInterFace();
            Object bean;
            if (beanContext != null) {
                bean = beanContext.getBean(boBeanClass);
            }else {
                bean = BeanFactory.getBean(boBeanClass);
            }
            if (bean instanceof LoadDataBean) {
                LoadDataBean loadDataBean = (LoadDataBean) bean;
                loadDataBeans.add(loadDataBean);
            }
            initMsgHandler(classMsgHandlerHashMap, boBeanClass, bean);
        }
        return Pair.of(classMsgHandlerHashMap, loadDataBeans);
    }

    public static void initMsgHandler(Map<String, MsgHandler> classMsgHandlerHashMap, Class<?> boBeanClass, Object bean) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        classes.add(boBeanClass);
        Class<?>[] interfaces = boBeanClass.getInterfaces();
        classes.addAll(Arrays.asList(interfaces));
        for (Class<?> anInterface : classes) {
            for (Method declaredMethod : anInterface.getDeclaredMethods()) {
                if (declaredMethod.getAnnotation(AskAnno.class) != null || declaredMethod.getAnnotation(Notice.class) != null) {
                    //创建处理器
                    Class<?> aClass = MsgHandlerEnhance.create(bean, declaredMethod);
                    try {
                        Class<?> parameterType = declaredMethod.getParameterTypes()[3];
                        MsgHandler handler = (MsgHandler) aClass.getConstructor(bean.getClass()).newInstance(bean);
                        classMsgHandlerHashMap.put(parameterType.getSimpleName(), handler);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static Class create(Object bean, Method declaredMethod) {
        Class<?> parameterType = declaredMethod.getParameterTypes()[3];
        String className = Type.getDescriptor(bean.getClass()).replaceAll(";", "_").replaceFirst("L", "") + parameterType.getSimpleName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {

        }

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;
        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", new String[]{"com/guangyu/cd003/projects/message/core/net/MsgHandler"});

        String boDescriptor = Type.getDescriptor(bean.getClass());
        {
            fv = cw.visitField(ACC_PRIVATE, "bo", boDescriptor, null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + boDescriptor + ")V", null, null);
            mv.visitParameter("bo", 0);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "bo", boDescriptor);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "recv", "(Lcom/guangyu/cd003/projects/message/core/net/MQContext;Ljava/lang/String;ILjava/lang/Object;)V", "(Lcom/guangyu/cd003/projects/message/core/net/MQContext<*>;Ljava/lang/String;ILjava/lang/Object;)V", null);
            mv.visitParameter("context", 0);
            mv.visitParameter("producerName", 0);
            mv.visitParameter("ix", 0);
            mv.visitParameter("msg", 0);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "bo", boDescriptor);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(parameterType));
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(bean.getClass()), declaredMethod.getName(), "(Lcom/guangyu/cd003/projects/message/core/net/MQContext;Ljava/lang/String;I" + Type.getDescriptor(parameterType) + ")V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(5, 5);
            mv.visitEnd();
        }
        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        return TheUnSafe.getUNSAFE().defineClass(null, bytes, 0, bytes.length, MsgHandlerEnhance.class.getClassLoader(), null);
    }


}
