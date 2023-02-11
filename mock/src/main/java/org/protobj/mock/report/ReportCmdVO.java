package org.protobj.mock.report;

import java.util.List;
import java.util.Map;

public class ReportCmdVO {

    private int cmd;

    private int rqstCount;

    private int successCount;

    private int averageRespTime1;

    private int averageRespTime2;
    private int minRespTime;

    private int maxRespTime;

    private float errRate;

    private int rqstPerSec1;

    private int rqstPerSec2;

    private Map<Integer, Integer> errCode;


    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getRqstCount() {
        return rqstCount;
    }

    public void setRqstCount(int rqstCount) {
        this.rqstCount = rqstCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getAverageRespTime1() {
        return averageRespTime1;
    }

    public void setAverageRespTime1(int averageRespTime1) {
        this.averageRespTime1 = averageRespTime1;
    }

    public int getAverageRespTime2() {
        return averageRespTime2;
    }

    public void setAverageRespTime2(int averageRespTime2) {
        this.averageRespTime2 = averageRespTime2;
    }

    public int getMinRespTime() {
        return minRespTime;
    }

    public void setMinRespTime(int minRespTime) {
        this.minRespTime = minRespTime;
    }

    public int getMaxRespTime() {
        return maxRespTime;
    }

    public void setMaxRespTime(int maxRespTime) {
        this.maxRespTime = maxRespTime;
    }

    public float getErrRate() {
        return errRate;
    }

    public void setErrRate(float errRate) {
        this.errRate = errRate;
    }

    public int getRqstPerSec1() {
        return rqstPerSec1;
    }

    public void setRqstPerSec1(int rqstPerSec1) {
        this.rqstPerSec1 = rqstPerSec1;
    }

    public int getRqstPerSec2() {
        return rqstPerSec2;
    }

    public void setRqstPerSec2(int rqstPerSec2) {
        this.rqstPerSec2 = rqstPerSec2;
    }

    public Map<Integer, Integer> getErrCode() {
        return errCode;
    }

    public void setErrCode(Map<Integer, Integer> errCode) {
        this.errCode = errCode;
    }

    public void merge(List<ReportCmdVO> value) {
        for (ReportCmdVO reportCmdVO : value) {
            this.rqstCount += reportCmdVO.getRqstCount();
            this.successCount += reportCmdVO.getSuccessCount();
            this.averageRespTime1 += reportCmdVO.getAverageRespTime1();
            this.averageRespTime2 += reportCmdVO.getAverageRespTime2();
            if (reportCmdVO.getMinRespTime() < this.minRespTime) {
                this.minRespTime = reportCmdVO.getMinRespTime();
            }
            if (reportCmdVO.getMaxRespTime() > this.maxRespTime) {
                this.maxRespTime = reportCmdVO.getMaxRespTime();
            }
            this.rqstPerSec1 += reportCmdVO.getRqstPerSec1();
            this.rqstPerSec2 += reportCmdVO.getRqstPerSec2();
            reportCmdVO.errCode.forEach((k, v) -> {
                this.errCode.merge(k, v, Integer::sum);
            });
        }
        float v = 1f - (float) successCount / (float) rqstCount;
        this.errRate = Float.parseFloat(String.format("%.4f", v));
    }
}
