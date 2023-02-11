package org.protobj.mock.net;

import java.util.concurrent.CompletionException;

public class CodeException extends RuntimeException {

    private int cmd;
    private int code;


    public CodeException(int cmd, int code) {
        super(String.format("cmd:%d code:%d", cmd, code));
        this.cmd = cmd;
        this.code = code;

    }


    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static int getCodeInEx(Throwable err) {
        if (err instanceof CompletionException){
            return getCodeInEx(err.getCause());
        }
        if (err instanceof CodeException) {
            return ((CodeException) err).getCode();
        }
        return -1;
    }
}
