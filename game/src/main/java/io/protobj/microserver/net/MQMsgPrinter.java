package io.protobj.microserver.net;

import com.guangyu.cd003.projects.microserver.log.ThreadLocalLoggerFactory;
import com.pv.common.utilities.common.GsonUtil;
import org.slf4j.Logger;

public class MQMsgPrinter {

    private final static Logger logger = ThreadLocalLoggerFactory.getLogger(MQMsgPrinter.class);

    private static boolean print = true;

    public void recvLog(String source, String target, String msgId, Object msg) {
        if (print && logger.isDebugEnabled())
            logger.debug("recv {}->{} msg:{}{}", source, target, msgId, GsonUtil.toJSONString(msg));
    }

    public void sendLog(String source, String target, String msgId, Object msg) {
        if (print && logger.isDebugEnabled() && !target.equals("Data/99"))
            logger.debug("send {}->{} msg:{}{}", source, target, msgId, GsonUtil.toJSONString(msg));
    }

    public void sendLog4Redirect(String source, String target, String msgId, Object msg) {
        if (print && logger.isDebugEnabled())
            logger.debug(" redirect send {}->{} msg:{}{}", source, target, msgId, GsonUtil.toJSONString(msg));
    }
}
