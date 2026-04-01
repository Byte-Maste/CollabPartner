package com.example.realtime_data_firebase;

public class TopicModel {
    public String title, description, videoId;
    public int progress;
    public String id;
    public String userId; // Owner of the topic
    public String sharedBy; // Who shared this topic (if shared)
    public String sharedTo; // Who received this topic (if shared)

    public TopicModel() {
    } // Required for Firestore

    public TopicModel(String id, String title, String description, String videoId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.videoId = videoId;
        this.progress = 0;
    }
}
