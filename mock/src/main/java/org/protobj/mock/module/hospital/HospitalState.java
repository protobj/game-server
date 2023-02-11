package org.protobj.mock.module.hospital;

public class HospitalState {
	
	public static final int IDLE = 0;//空闲
    public static final int CURE = 1;//治疗|正在治疗
    public static final int RQST_CURE = 2;//请求治疗
    public static final int RQST_IMMEDIATE = 3;//请求快速治疗
    public static final int CURE_COMPLETE = 4;//完成治疗
    public static final int RQST_COLLECT = 5;//请求收兵
    public static final int HAVE_NO_RSRC = 6;//资源不足
    private int curState = IDLE;

    public void setCurState(int curState) {
        this.curState = curState;
//        print();
    }
    
    private void print() {
        if (curState == IDLE) {
            System.err.println("IDLE");
        } else if (curState == RQST_CURE) {
            System.err.println("RQST_CURE");
        } else if (curState == CURE) {
            System.err.println("CURE");
        } else if (curState == RQST_IMMEDIATE) {
            System.err.println("RQST_IMMEDIATE");
        } else if (curState == CURE_COMPLETE) {
            System.err.println("CURE_COMPLETE");
        } else if (curState == RQST_COLLECT) {
            System.err.println("RQST_COLLECT");
        } else if (curState == HAVE_NO_RSRC) {
            System.err.println("HAVE_NO_RSRC");
        }
    }

    public int getCurState() {
        return curState;
    }
}
