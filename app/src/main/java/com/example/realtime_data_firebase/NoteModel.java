package com.example.realtime_data_firebase;

public class NoteModel {
    public String noteId, text, senderId, senderName;
    public long timestamp;

    public NoteModel() {
    }

    public NoteModel(String noteId, String text, String senderId, String senderName, long timestamp) {
        this.noteId = noteId;
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = timestamp;
    }
}
