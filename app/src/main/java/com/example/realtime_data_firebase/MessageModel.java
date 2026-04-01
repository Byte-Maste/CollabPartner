package com.example.realtime_data_firebase;

public class MessageModel {
    public String messageId, senderId, text, type, fileName; // type = "text", "file", "image"
    public long timestamp;

    public MessageModel() {
    }

    public MessageModel(String messageId, String senderId, String text, String type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}