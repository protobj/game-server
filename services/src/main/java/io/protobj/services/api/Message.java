package io.protobj.services.api;

public class Message {

    private Header header;

    private Content content;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Content getBody() {
        return content;
    }

    public void setBody(Content content) {
        this.content = content;
    }


    public static class Header {

        private int cmd;

        public int getCmd() {
            return cmd;
        }

        public void setCmd(int cmd) {
            this.cmd = cmd;
        }
    }

    public static interface Content {

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
}
