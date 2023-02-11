package org.protobj.mock.net;

import java.util.concurrent.CompletableFuture;

public class RqstFuture extends CompletableFuture<Integer> {

    //请求码
    int rqstCmd;
    //请求事件
    volatile long rqstTime;

    public RqstFuture(int rqstCmd) {
        this.rqstCmd = rqstCmd;
    }

    public RqstFuture(int rqstCmd, long rqstTime) {
        this.rqstCmd = rqstCmd;
        this.rqstTime = rqstTime;
    }

    public int getRqstCmd() {
        return rqstCmd;
    }

    public void setRqstCmd(int rqstCmd) {
        this.rqstCmd = rqstCmd;
    }

    public long getRqstTime() {
        return rqstTime;
    }

    public synchronized void setRqstTime(long rqstTime) {
        this.rqstTime = rqstTime;
    }
}
