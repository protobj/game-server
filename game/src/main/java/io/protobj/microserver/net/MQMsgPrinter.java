package io.protobj.microserver.net;

import io.protobj.util.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQMsgPrinter {

    private final static Logger logger = LoggerFactory.getLogger(MQMsgPrinter.class);

    private static boolean print = true;

    public void recvLog(String source, String target, String msgId, Object msg) {
        if (print && logger.isDebugEnabled())
            logger.debug("recv {}->{} msg:{}{}", source, target, msgId, Jackson.INSTANCE.encode(msg));
    }

    public void sendLog(String source, String target, String msgId, Object msg) {
        if (print && logger.isDebugEnabled() && !target.equals("Data/99"))
            logger.debug("send {}->{} msg:{}{}", source, target, msgId, Jackson.INSTANCE.encode(msg));
    }

    public void sendLog4Redirect(String source, String target, String msgId, Object msg) {
        if (print && logger.isDebugEnabled())
            logger.debug(" redirect send {}->{} msg:{}{}", source, target, msgId, Jackson.INSTANCE.encode(msg));
    }
}
