package com.example.realtime_data_firebase;

import java.util.List;

public class JobModel {
    public String jobId, creatorId;
    public String title, description, company, location, type, budget, deadline;
    public String skills; // Stored as comma separated string for simplicity
    public long postedDate;
    public int applicantCount;

    public JobModel() {} // Required for Firestore

    public JobModel(String jobId, String creatorId, String title, String description,
                    String company, String location, String type, String budget,
                    String deadline, String skills) {
        this.jobId = jobId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.company = company;
        this.location = location;
        this.type = type;
        this.budget = budget;
        this.deadline = deadline;
        this.skills = skills;
        this.postedDate = System.currentTimeMillis();
        this.applicantCount = 0;
    }
}