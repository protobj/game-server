package io.protobj.network;

import io.protobj.event.Response;

import java.util.List;

public class MessageListResponse implements Response {

    private List<Object> messageList;


    public static MessageListResponse valueOf() {
        return new MessageListResponse();
    }

    public List<Object> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Object> messageList) {
        this.messageList = messageList;
    }
}
