package io.protobj.services.api;

public class Message {

    private Header header;

    private Content content;

    public static Message error(Header header, int errorCode, String errorMessage) {
        Message message = new Message();
        message.setHeader(header);
        message.setContent(new ErrorData(errorCode, errorMessage));
        return message;
    }

    public static Message New(Header header, Content content) {
        Message message = new Message();
        message.setHeader(header);
        message.setContent(content);
        return message;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }


    public static class Header {

        private int cmd;

        private boolean error;

        public int getCmd() {
            return cmd;
        }

        public void setCmd(int cmd) {
            this.cmd = cmd;
        }

        public boolean isError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }
    }

    public interface Content {

    }

    public static class ErrorData implements Content {

        private final int code;

        private final String detail;


        public ErrorData(int code, String detail) {
            this.code = code;
            this.detail = detail;
        }

        public ErrorData() {
            this.code = 0;
            this.detail = "";
        }

        public int getCode() {
            return code;
        }

        public String getDetail() {
            return detail;
        }
    }

    public interface SidContent extends Content {
        int sid();
    }

    public interface SidsContent extends Content {
        int[] sids();
    }

    public interface SlotContent extends Content {
        long slotKey();
    }
}
