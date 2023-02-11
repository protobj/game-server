package org.protobj.mock.report;

import com.guangyu.cd003.projects.mock.plan.RandomRqstPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportVO {

    private long onlineCount;

    private int registerCountPerSec;

    private int rqstPerSec;

    private List<ReportCmdVO> reportCmdList = new ArrayList<>();


    public long getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(long onlineCount) {
        this.onlineCount = onlineCount;
    }

    public int getRegisterCountPerSec() {
        return registerCountPerSec;
    }

    public void setRegisterCountPerSec(int registerCountPerSec) {
        this.registerCountPerSec = registerCountPerSec;
    }

    public int getRqstPerSec() {
        return rqstPerSec;
    }

    public void setRqstPerSec(int rqstPerSec) {
        this.rqstPerSec = rqstPerSec;
    }

    public List<ReportCmdVO> getReportCmdList() {
        return reportCmdList;
    }

    public void setReportCmdList(List<ReportCmdVO> reportCmdList) {
        this.reportCmdList = reportCmdList;
    }

    public String toCsv() {
        StringBuilder content = new StringBuilder();
        content.append("在线人数：").append(onlineCount).append("\n");
        content.append("每秒注册用户量：").append(registerCountPerSec).append("\n");
        content.append("每秒请求数：").append(rqstPerSec).append("\n");
        content.append("命令码,").append("描述,").append("请求次数,")//
                .append("成功次数,").append("平均响应时间1,").append("平均响应时间2,")//
                .append("最小响应时间,").append("最大响应时间,").append("错误率,")//
                .append("每秒请求数1,").append("每秒请求数2,").append("错误码\n");//
        for (ReportCmdVO value : getReportCmdList()) {
            content.append(value.getCmd()).append(",").append(RandomRqstPlan.rqstDescMap.get(value.getCmd())).append(",").append(value.getRqstCount())//
                    .append(",").append(value.getSuccessCount()).append(",").append(value.getAverageRespTime1()).append(",")//
                    .append(value.getAverageRespTime2()).append(",").append(value.getMinRespTime()).append(",").append(value.getMaxRespTime()).append(",").append(value.getErrRate()).append(",").append(value.getRqstPerSec1()).append(",").append(value.getRqstPerSec2()).append(",").append(toString(value.getErrCode())).append("\n");
        }

        return content.toString();
    }

    private String toString(Map<Integer, Integer> errCode) {
        StringBuilder sb = new StringBuilder();
        errCode.forEach((k, v) -> {
            sb.append(k).append("->").append(v).append(" ");
        });
        return sb.toString();
    }

    public void merge(List<ReportVO> reportVO) {
        for (ReportVO vo : reportVO) {
            this.onlineCount += vo.getOnlineCount();
            this.registerCountPerSec += vo.getRegisterCountPerSec();
            this.rqstPerSec += vo.getRqstPerSec();
        }
        this.registerCountPerSec = (this.registerCountPerSec / (1 + reportVO.size()));
        this.rqstPerSec = (this.rqstPerSec / (1 + reportVO.size()));
        for (ReportVO vo : reportVO) {
            List<ReportCmdVO> reportCmdList1 = vo.getReportCmdList();
            this.reportCmdList.addAll(reportCmdList1);
        }
        Map<Integer, List<ReportCmdVO>> collect = this.reportCmdList.stream().collect(Collectors.groupingBy(ReportCmdVO::getCmd, Collectors.toList()));
        this.reportCmdList.clear();
        for (List<ReportCmdVO> value : collect.values()) {
            ReportCmdVO remove = value.remove(0);
            remove.merge(value);
            this.reportCmdList.add(remove);
        }
    }
}
