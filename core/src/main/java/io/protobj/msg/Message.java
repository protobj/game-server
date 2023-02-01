package io.protobj.msg;

import io.protobj.network.gateway.backend.client.session.Session;

//所有消息的父类
public class Message {

    private transient Session session;

    private int messageIndex;


    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public int getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(int messageIndex) {
        this.messageIndex = messageIndex;
    }
}
