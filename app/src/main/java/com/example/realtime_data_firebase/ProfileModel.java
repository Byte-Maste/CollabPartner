package com.example.realtime_data_firebase;

import java.util.List;

public class ProfileModel {
    public String name, handle, headline, location, about, website, status;
    public String github, linkedin, twitter;
    public List<String> skills;
    public long lastSeen;
    public int projectCount, connectionCount, viewCount;
    public int streakCount;
    public long lastActiveTime;
    public String profileImageUrl;

    public ProfileModel() {
    } // Required for Firestore

    public ProfileModel(String name, String handle, String headline, String location, String about) {
        this.name = name;
        this.handle = handle;
        this.headline = headline;
        this.location = location;
        this.about = about;
        // Default stats for new users
        this.projectCount = 0;
        this.connectionCount = 0;
        this.viewCount = 0;
    }
}