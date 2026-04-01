package com.example.realtime_data_firebase;

public class EventModel {
    // Added type (Online/Offline) and meetingLink
    public String title, description, date, time, location, eventId, creatorId, type, meetingLink;

    public EventModel() {
    }

    public java.util.List<String> attendees;

    public EventModel(String title, String description, String date, String time, String location, String eventId,
            String creatorId, String type, String meetingLink) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.location = location;
        this.eventId = eventId;
        this.creatorId = creatorId;
        this.type = type;
        this.meetingLink = meetingLink;
        this.attendees = new java.util.ArrayList<>();
    }
}
