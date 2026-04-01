package com.example.realtime_data_firebase;

public class CollabModel {
    public String userId, name, headline, location, about;
    // We reuse the same fields from the User collection

    public CollabModel() {} // Required for Firestore

    public CollabModel(String userId, String name, String headline, String location, String about) {
        this.userId = userId;
        this.name = name;
        this.headline = headline;
        this.location = location;
        this.about = about;
    }
}