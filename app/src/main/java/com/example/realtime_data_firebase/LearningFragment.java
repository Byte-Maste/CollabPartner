package com.example.realtime_data_firebase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LearningFragment extends Fragment {

    RecyclerView recyclerView;
    List<TopicModel> list;
    TopicAdapter adapter;
    com.google.android.material.floatingactionbutton.FloatingActionButton fab;
    com.google.firebase.firestore.FirebaseFirestore db;

    public LearningFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_learning, container, false);

        recyclerView = view.findViewById(R.id.learningRecycler);
        fab = view.findViewById(R.id.fabAddTopic);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        list = new ArrayList<>();
        adapter = new TopicAdapter(list, getContext(), new TopicAdapter.OnTopicClickListener() {
            @Override
            public void onTopicClick(TopicModel model) {
                android.content.Intent intent = new android.content.Intent(getContext(), VideoPlayerActivity.class);
                intent.putExtra("videoId", model.videoId);
                intent.putExtra("videoTitle", model.title);
                startActivity(intent);
            }

            @Override
            public void onTopicLongClick(TopicModel model) {
                // Show Edit/Delete Options
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Options")
                        .setItems(new String[] { "Edit", "Delete" }, (dialog, which) -> {
                            if (which == 0) {
                                showAddEditDialog(model);
                            } else {
                                db.collection("LearningTopics").document(model.id).delete();
                            }
                        }).show();
            }

            @Override
            public void onShareTopic(TopicModel model) {
                showShareDialog(model);
            }
        });
        recyclerView.setAdapter(adapter);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        loadTopics();

        fab.setOnClickListener(v -> showAddEditDialog(null));

        return view;
    }

    private void loadTopics() {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("LearningTopics")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null)
                        return;
                    if (value == null)
                        return;

                    list.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        list.add(doc.toObject(TopicModel.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddEditDialog(TopicModel model) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_topic, null);
        builder.setView(view);

        android.widget.TextView title = view.findViewById(R.id.dialogTitle);
        android.widget.EditText inputTitle = view.findViewById(R.id.inputTopicTitle);
        android.widget.EditText inputDesc = view.findViewById(R.id.inputTopicDesc);
        android.widget.EditText inputVideoId = view.findViewById(R.id.inputVideoId);
        android.widget.Button btnSave = view.findViewById(R.id.btnSaveTopic);

        android.app.Dialog dialog = builder.create();

        if (model != null) {
            title.setText("Edit Topic");
            inputTitle.setText(model.title);
            inputDesc.setText(model.description);
            inputVideoId.setText(model.videoId);
        }

        btnSave.setOnClickListener(v -> {
            String t = inputTitle.getText().toString().trim();
            String d = inputDesc.getText().toString().trim();
            String vid = inputVideoId.getText().toString().trim();

            if (t.isEmpty() || d.isEmpty() || vid.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "All fields required", android.widget.Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if (model == null) {
                // Add new
                String id = db.collection("LearningTopics").document().getId();
                String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                TopicModel newTopic = new TopicModel(id, t, d, vid);
                newTopic.userId = currentUserId;
                db.collection("LearningTopics").document(id).set(newTopic);
            } else {
                // Update
                model.title = t;
                model.description = d;
                model.videoId = vid;
                db.collection("LearningTopics").document(model.id).set(model);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showShareDialog(TopicModel topic) {
        // Fetch user's connections from private chats
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Step 1: Get all private chats to find connected friends
        db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .whereEqualTo("type", "private")
                .get()
                .addOnSuccessListener(chats -> {
                    java.util.List<String> friendIds = new java.util.ArrayList<>();

                    // Extract friend IDs from chat participants
                    for (com.google.firebase.firestore.DocumentSnapshot chat : chats) {
                        java.util.List<String> participants = (java.util.List<String>) chat.get("participants");
                        if (participants != null) {
                            for (String id : participants) {
                                if (!id.equals(currentUserId) && !friendIds.contains(id)) {
                                    friendIds.add(id);
                                }
                            }
                        }
                    }

                    if (friendIds.isEmpty()) {
                        android.widget.Toast
                                .makeText(getContext(), "No connections found. Start chatting with friends first!",
                                        android.widget.Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    // Step 2: Fetch user details for these friend IDs
                    db.collection("Users").get().addOnSuccessListener(users -> {
                        java.util.List<String> connectionNames = new java.util.ArrayList<>();
                        java.util.List<String> connectionIds = new java.util.ArrayList<>();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : users) {
                            if (friendIds.contains(doc.getId())) {
                                String name = doc.getString("name");
                                if (name == null)
                                    name = doc.getString("fullName");
                                if (name != null) {
                                    connectionIds.add(doc.getId());
                                    connectionNames.add(name);
                                }
                            }
                        }

                        if (connectionNames.isEmpty()) {
                            android.widget.Toast.makeText(getContext(), "Could not load friend details",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Step 3: Show dialog to select connections
                        boolean[] selectedConnections = new boolean[connectionNames.size()];

                        new android.app.AlertDialog.Builder(getContext())
                                .setTitle("Share \"" + topic.title + "\" with:")
                                .setMultiChoiceItems(connectionNames.toArray(new String[0]), selectedConnections,
                                        (dialog, which, isChecked) -> selectedConnections[which] = isChecked)
                                .setPositiveButton("Share", (dialog, which) -> {
                                    int shareCount = 0;
                                    // Share with selected connections
                                    for (int i = 0; i < selectedConnections.length; i++) {
                                        if (selectedConnections[i]) {
                                            shareTopic(topic, connectionIds.get(i), connectionNames.get(i));
                                            shareCount++;
                                        }
                                    }
                                    if (shareCount > 0) {
                                        android.widget.Toast.makeText(getContext(),
                                                "Topic shared with " + shareCount + " friend(s)!",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast
                            .makeText(getContext(), "Failed to load connections", android.widget.Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void shareTopic(TopicModel topic, String recipientId, String recipientName) {
        // First, check if this topic already exists for the recipient
        db.collection("LearningTopics")
                .whereEqualTo("userId", recipientId)
                .whereEqualTo("videoId", topic.videoId)
                .whereEqualTo("title", topic.title)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Topic already exists for this user
                        android.widget.Toast.makeText(getContext(),
                                recipientName + " already has this topic in their learning path",
                                android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Topic doesn't exist, create it
                    String newTopicId = db.collection("LearningTopics").document().getId();

                    java.util.Map<String, Object> sharedTopic = new java.util.HashMap<>();
                    sharedTopic.put("id", newTopicId);
                    sharedTopic.put("title", topic.title);
                    sharedTopic.put("description", topic.description);
                    sharedTopic.put("videoId", topic.videoId);
                    sharedTopic.put("progress", 0); // Reset progress for new user
                    sharedTopic.put("userId", recipientId); // Set recipient as owner
                    sharedTopic.put("sharedBy",
                            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid());
                    sharedTopic.put("sharedTo", recipientId);
                    sharedTopic.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    db.collection("LearningTopics").document(newTopicId).set(sharedTopic)
                            .addOnSuccessListener(aVoid -> {
                                // Optionally send a notification to the recipient
                                sendShareNotification(recipientId, topic.title);
                            })
                            .addOnFailureListener(e -> {
                                android.widget.Toast.makeText(getContext(), "Failed to share with " + recipientName,
                                        android.widget.Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(getContext(), "Failed to check existing topics",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void sendShareNotification(String recipientId, String topicTitle) {
        // Create a notification document (optional - for future notification system)
        String currentUserName = "A friend"; // You can fetch from user profile

        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("type", "learning_share");
        notification.put("message", currentUserName + " shared a learning topic: " + topicTitle);
        notification.put("recipientId", recipientId);
        notification.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        notification.put("read", false);

        db.collection("Notifications").add(notification);
    }
}
