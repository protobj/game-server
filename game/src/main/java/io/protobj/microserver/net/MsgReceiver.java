package io.protobj.microserver.net;

/**
 * Created on 2021/7/7.
 *
 * @author chen qiang
 */
public interface MsgReceiver extends LoadDataHandler {
    void recv(MQContext<?> context, String producerName, int ix, String msgId, Object msgData);
}
