package io.protobj.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {

    public static Field makeAccessible(Field field) {
        if (!field.trySetAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    public static Constructor makeAccessible(Constructor constructor) {
        if (!constructor.trySetAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor;
    }

    public static Method makeAccessible(Method method) {
        if (!method.trySetAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
