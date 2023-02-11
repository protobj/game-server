package io.protobj.microserver.net;

/**
 * Created on 2021/6/23.
 *
 * @author chen qiang
 * 接收處理推送消息
 */
public interface MsgHandler {

    void recv(MQContext<?> context, String producerName, int ix, Object msg);
}
