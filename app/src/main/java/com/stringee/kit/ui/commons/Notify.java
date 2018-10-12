package com.stringee.kit.ui.commons;

public enum Notify {
    CONVERSATION_ADDED("com.stringee.conversation.added"), CONVERSATION_UPDATED("com.stringee.conversation.updated"), CONNECTION_CONNECTED("com.stringee.connection.connected");

    private String value;

    private Notify(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
