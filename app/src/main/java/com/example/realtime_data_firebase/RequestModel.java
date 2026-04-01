package com.example.realtime_data_firebase;

public class RequestModel {
    public String requestId, senderId, senderName, senderHeadline, receiverId, status;
    public long timestamp;

    public RequestModel() {}

    public RequestModel(String requestId, String senderId, String senderName, String senderHeadline, String receiverId) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderHeadline = senderHeadline;
        this.receiverId = receiverId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }
}