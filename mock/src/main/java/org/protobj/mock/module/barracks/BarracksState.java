package org.protobj.mock.module.barracks;

public class BarracksState {

    public static final int IDLE = 0;//空闲
    public static final int TRAINING = 1;//响应训练|正在训练
    public static final int TRAIN_COMPLETE = 2;//训练完成
    public static final int RQST_TRAIN = 3;//请求训练
    public static final int RQST_SPEEDUP_ING = 4;//请求加速
    public static final int RQST_IMMEDIATE = 5;//请求立即完成
    public static final int RQST_COLLECT = 6;//请求收取

    public static final int HAVE_NO_RSRC = 7;//资源不足
    private int curState = IDLE;

    public void setCurState(int curState) {
        this.curState = curState;
//        print();
    }

    private void print() {
        if (curState == IDLE) {
            System.err.println("IDLE");
        } else if (curState == TRAINING) {
            System.err.println("TRAINING");
        } else if (curState == TRAIN_COMPLETE) {
            System.err.println("TRAIN_COMPLETE");
        } else if (curState == RQST_TRAIN) {
            System.err.println("RQST_TRAIN");
        } else if (curState == RQST_SPEEDUP_ING) {
            System.err.println("RQST_SPEEDUP_ING");
        } else if (curState == RQST_IMMEDIATE) {
            System.err.println("RQST_IMMEDIATE");
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
