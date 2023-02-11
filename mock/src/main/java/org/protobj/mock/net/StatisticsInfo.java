package org.protobj.mock.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsInfo {

    public final AtomicLong msgCount = new AtomicLong();
    public long startTime = 0;
    public long endTime = 0;

    public AtomicBoolean start = new AtomicBoolean();

    public void start() {
        start.compareAndSet(false, true);
    }

    public ConcurrentMap<Integer, CmdStatisticsInfo> cmdStatisticsInfoMap = new ConcurrentHashMap<>();


    public void rqst(int cmd) {
        if (!start.get()) {
            return;
        }
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
    }

    public void resp(int cmd, long rqstTime) {
        if (!start.get()) {
            return;
        }
        msgCount.incrementAndGet();
        this.endTime = System.currentTimeMillis();
        CmdStatisticsInfo cmdStatisticsInfo = cmdStatisticsInfoMap.computeIfAbsent(cmd, CmdStatisticsInfo::new);
        synchronized (cmdStatisticsInfo) {
            cmdStatisticsInfo.succResp(rqstTime);
        }
    }

    public void respErr(int cmd, long rqstTime, int code) {
        if (!start.get() || rqstTime == 0) {
            return;
        }
        msgCount.incrementAndGet();
        this.endTime = System.currentTimeMillis();
        CmdStatisticsInfo cmdStatisticsInfo = cmdStatisticsInfoMap.computeIfAbsent(cmd, CmdStatisticsInfo::new);
        synchronized (cmdStatisticsInfo) {
            cmdStatisticsInfo.errResp(rqstTime, code);
        }
    }

    public static class CmdStatisticsInfo {
        //请求码
        public int cmd;

        public long startTime;

        public long endTime;
        //请求次数
        public AtomicInteger rqstCount = new AtomicInteger();
        //响应总时间
        public AtomicLong totalRespTime = new AtomicLong();

        //成功次数
        public AtomicInteger successCount = new AtomicInteger();

        //返回错误码->错误次数
        public ConcurrentMap<Integer, Integer> errorCodeMap = new ConcurrentHashMap<>();

        public int min = Integer.MAX_VALUE;
        public int max = Integer.MIN_VALUE;

        //平均响应时间1
        public int getAverage1() {
            double respTime = this.endTime - this.startTime;
            double rqstCount = this.successCount.get();
            return (int) (respTime / rqstCount);
        }

        //平均响应时间2
        public int getAverage2() {
            double respTime = totalRespTime.get();
            double rqstCount = this.successCount.get();
            return (int) (respTime / rqstCount);
        }

        //最小响应时间
        public int getMin() {
            return min;
        }

        //最大响应时间
        public int getMax() {
            return max;
        }

        //错误率
        public float getErrorRate() {
            float v = (float) (1 - successCount.get() / ((double) rqstCount.get()));
            return Float.parseFloat(String.format("%.4f", v));
        }

        //每秒完成请求数1
        public int getThroughput1() {
            double respTime = this.endTime - this.startTime;
            double rqstCount = this.successCount.get();
            return (int) (1000d * (rqstCount / respTime));
        }

        //每秒完成请求数2
        public int getThroughput2() {
            double respTime = this.totalRespTime.get();
            double rqstCount = this.successCount.get();
            return (int) (1000d * (rqstCount / respTime));
        }

        public CmdStatisticsInfo(Integer cmd) {
            this.cmd = cmd;
            startTime = System.currentTimeMillis();
        }

        public void succResp(long rqstStartTime) {
            resp(rqstStartTime);
            successCount.incrementAndGet();
        }

        public void errResp(long rqstStartTime, int code) {
            resp(rqstStartTime);
            errorCodeMap.putIfAbsent(code, 0);
            errorCodeMap.merge(code, 1, Integer::sum);
        }

        private void resp(long rqstStartTime) {
            long timeMillis = System.currentTimeMillis();
            endTime = timeMillis;
            rqstCount.incrementAndGet();
            long respTime = timeMillis - rqstStartTime;
            totalRespTime.addAndGet(respTime);
            if (respTime < min) {
                this.min = (int) respTime;
            }
            if (respTime > max) {
                this.max = (int) respTime;
            }
        }
    }
}
