package io.protobj.microserver.net.impl;

import io.protobj.microserver.net.MQMsg;
import io.protobj.microserver.net.MQSerilizer;

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

    }

    @Override
    public byte[] encode(Object msg) {
        return null;
    }

    @Override
    public Object decode(String simpleName, byte[] bytes) {
        Class<?> clazz = simpleName2Class.get(simpleName);
        if (clazz == null) {
            throw new RuntimeException("消息未注册：" + simpleName);
        }
        return null;
    }

}
