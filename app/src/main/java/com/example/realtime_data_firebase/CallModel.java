package com.example.realtime_data_firebase;

public class CallModel {
    public String callId, callerId, callerName, callerAvatar, receiverId, roomId, status;
    public long timestamp;

    // Default constructor for Firestore
    public CallModel() {
    }

    public CallModel(String callId, String callerId, String callerName, String callerAvatar, String receiverId,
            String roomId, long timestamp) {
        this.callId = callId;
        this.callerId = callerId;
        this.callerName = callerName;
        this.callerAvatar = callerAvatar;
        this.receiverId = receiverId;
        this.roomId = roomId;
        this.status = "ringing"; // Default status
        this.timestamp = timestamp;
    }
}
