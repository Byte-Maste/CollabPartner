package com.example.realtime_data_firebase;

import java.util.List;

public class ChatModel {
    public String chatId;
    public String chatName; // For groups. For 1-on-1, this is usually null (handled dynamically)
    public String chatImage; // URL for group icon
    public List<String> participants; // List of User UIDs
    public String lastMessage;
    public long lastMessageTime;
    public String type; // "private" or "group"

    public ChatModel() {}

    // Constructor for creating a new chat
    public ChatModel(String chatId, List<String> participants, String type, String chatName) {
        this.chatId = chatId;
        this.participants = participants;
        this.type = type;
        this.chatName = chatName;
        this.lastMessage = "Start chatting!";
        this.lastMessageTime = System.currentTimeMillis();
    }
}