package io.protobj.microserver.net;

/**
 * Created on 2021/7/7.
 *
 * @author chen qiang
 */
public interface MQSerilizer{

    public byte[] encode(Object msg);

    public Object decode(String simpleName, byte[] bytes);
}
