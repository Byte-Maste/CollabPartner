package com.example.realtime_data_firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to manage dynamic profile count updates
 */
public class ProfileCountManager {

    private static ProfileCountManager instance;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ProfileCountManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static ProfileCountManager getInstance() {
        if (instance == null) {
            instance = new ProfileCountManager();
        }
        return instance;
    }

    /**
     * Update project count for current user
     */
    public void updateProjectCount() {
        String uid = auth.getCurrentUser().getUid();

        // Projects are stored in Users/{uid}/Projects subcollection
        db.collection("Users")
                .document(uid)
                .collection("Projects")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    updateUserField(uid, "projectCount", count);
                });
    }

    /**
     * Update connection count for current user
     */
    public void updateConnectionCount() {
        String uid = auth.getCurrentUser().getUid();

        // Count private chats (connections)
        db.collection("Chats")
                .whereArrayContains("participants", uid)
                .whereEqualTo("type", "private")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    updateUserField(uid, "connectionCount", count);
                });
    }

    /**
     * Increment profile view count
     */
    public void incrementProfileView(String profileUserId) {
        db.collection("Users").document(profileUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    int currentViews = 0;
                    if (doc.exists() && doc.contains("viewCount")) {
                        Long views = doc.getLong("viewCount");
                        currentViews = views != null ? views.intValue() : 0;
                    }
                    updateUserField(profileUserId, "viewCount", currentViews + 1);
                });
    }

    /**
     * Helper method to update a single field in user document
     */
    private void updateUserField(String uid, String field, int value) {
        Map<String, Object> data = new HashMap<>();
        data.put(field, value);

        db.collection("Users").document(uid)
                .update(data)
                .addOnFailureListener(e -> {
                    // Silently fail - counts are not critical
                });
    }

    /**
     * Refresh all counts for current user
     */
    public void refreshAllCounts() {
        updateProjectCount();
        updateConnectionCount();
    }
}
