package io.protobj.microserver.net.impl;

import com.guangyu.cd003.projects.message.core.net.MQMsg;
import com.guangyu.cd003.projects.message.core.net.MQSerilizer;
import com.pv.common.utilities.reflc.ReflcUtil;
import com.pv.common.utilities.serialization.protostuff.ProtostuffUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created on 2021/7/7.
 *
 * @author chen qiang
 */
public class MQSerilizerImpl implements MQSerilizer {

    private Map<String, Class<?>> simpleName2Class = new HashMap<>();

    public MQSerilizerImpl() {
        Set<Class<?>> classes = ReflcUtil.forClassAnnotatedWith("com.guangyu.cd003.projects", MQMsg.class);
        for (Class<?> aClass : classes) {
            String simpleName = aClass.getSimpleName();
            if (simpleName2Class.put(simpleName, aClass) != null) {
                throw new RuntimeException("消息重名了 +" + simpleName);
            }
        }
    }

    @Override
    public byte[] encode(Object msg) {
        return ProtostuffUtil.ser(msg);
    }

    @Override
    public Object decode(String simpleName, byte[] bytes) {
        Class<?> clazz = simpleName2Class.get(simpleName);
        if (clazz == null) {
            throw new RuntimeException("消息未注册：" + simpleName);
        }
        return ProtostuffUtil.deser(bytes, clazz);
    }

}
