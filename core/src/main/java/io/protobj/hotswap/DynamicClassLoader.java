package io.protobj.hotswap;

import java.util.HashMap;
import java.util.Map;

public class DynamicClassLoader extends ClassLoader {

    private Map<String, CompiledCode> customCompiledCode = new HashMap<>();

    public DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void addCode(CompiledCode cc) {
        customCompiledCode.put(cc.getName(), cc);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        CompiledCode cc = customCompiledCode.get(name);
        if (cc == null) {
            return super.findClass(name);
        }
        byte[] byteCode = cc.getByteCode();
        return defineClass(name, byteCode, 0, byteCode.length);
    }

    public byte[] getClassByteArray(String name) {
        return customCompiledCode.get(name).getByteCode();
    }

    public Map<String, byte[]> getCustomCompiledCode() {
        Map<String, byte[]> result = new HashMap<>();
        for (Map.Entry<String, CompiledCode> stringCompiledCodeEntry : customCompiledCode.entrySet()) {
            result.put(stringCompiledCodeEntry.getKey(), stringCompiledCodeEntry.getValue().getByteCode());
        }
        return result;
    }
}
