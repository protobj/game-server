package io.protobj.enhance;

import javassist.ClassPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnhanceClassCache {


    private ClassPool classPool;

    private Map<String, Class> classMap;


    public EnhanceClassCache() {
        this.classPool = new ClassPool(true);
        this.classMap = new ConcurrentHashMap<>();
    }


    public ClassPool getClassPool() {
        return classPool;
    }

    public Class getEnhanceClass(String classname) {
        return classMap.get(classname);
    }

    public void putEnhanceClass(Class<?> clz) {
        this.classMap.put(clz.getName(), clz);
    }
}
