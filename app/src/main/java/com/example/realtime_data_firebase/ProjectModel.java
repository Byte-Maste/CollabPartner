package com.example.realtime_data_firebase;

public class ProjectModel {
    public String title, description, github, projectId,deleteBtn;

    public ProjectModel() {} // Firestore needs empty constructor

    public ProjectModel(String title, String description, String github, String projectId) {
        this.title = title;
        this.description = description;
        this.github = github;
        this.projectId = projectId;
    }
}
