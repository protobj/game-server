package org.protobj.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 * <p>
 * 记录消息耗时等等
 */
public class MockRecord{

    private static final Logger logger = LoggerFactory.getLogger(MockRecord.class);

    public AtomicLong rqstCount = new AtomicLong();
    public AtomicLong rqstTime = new AtomicLong();

    public void print() {
//        logger.error(" 请求平均耗时：{}ms", (rqstTime.get() / rqstCount.get()) / 1000_000L);
    }
}
