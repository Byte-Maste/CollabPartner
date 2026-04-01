package com.example.realtime_data_firebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoPlayerActivity extends AppCompatActivity {

    Button btnPlayVideo;
    TextView videoTitle;
    RecyclerView notesRecycler;
    EditText inputNote;
    ImageView btnAddNote;

    String videoId, title;
    FirebaseFirestore db;
    FirebaseAuth auth;
    List<NoteModel> noteList;
    NoteAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoId = getIntent().getStringExtra("videoId");
        title = getIntent().getStringExtra("videoTitle");

        btnPlayVideo = findViewById(R.id.btnPlayVideo);
        videoTitle = findViewById(R.id.videoTitle);
        notesRecycler = findViewById(R.id.notesRecycler);
        inputNote = findViewById(R.id.inputNote);
        btnAddNote = findViewById(R.id.btnAddNote);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        videoTitle.setText(title);

        // Extract video ID from URL if needed
        videoId = extractVideoId(videoId);

        // Open YouTube when button is clicked
        btnPlayVideo.setOnClickListener(v -> openYouTubeVideo());

        setupNotes();

        btnAddNote.setOnClickListener(v -> sendNote());
    }

    /**
     * Extract video ID from various YouTube URL formats
     * Supports: youtu.be/ID, youtube.com/watch?v=ID, youtube.com/embed/ID
     */
    private String extractVideoId(String input) {
        if (input == null || input.isEmpty())
            return input;

        // Already just an ID (11 characters, no special chars)
        if (input.length() == 11 && !input.contains("/") && !input.contains("?")) {
            return input;
        }

        // youtu.be/ID or youtu.be/ID?si=...
        if (input.contains("youtu.be/")) {
            String[] parts = input.split("youtu.be/");
            if (parts.length > 1) {
                String idPart = parts[1];
                // Remove query parameters if present
                if (idPart.contains("?")) {
                    idPart = idPart.split("\\?")[0];
                }
                return idPart;
            }
        }

        // youtube.com/watch?v=ID
        if (input.contains("youtube.com/watch?v=")) {
            String[] parts = input.split("v=");
            if (parts.length > 1) {
                String idPart = parts[1];
                // Remove additional parameters
                if (idPart.contains("&")) {
                    idPart = idPart.split("&")[0];
                }
                return idPart;
            }
        }

        // youtube.com/embed/ID
        if (input.contains("youtube.com/embed/")) {
            String[] parts = input.split("embed/");
            if (parts.length > 1) {
                String idPart = parts[1];
                if (idPart.contains("?")) {
                    idPart = idPart.split("\\?")[0];
                }
                return idPart;
            }
        }

        // If no pattern matched, return original
        return input;
    }

    /**
     * Open Video in-app fullscreen modal
     */
    private void openYouTubeVideo() {
        if (!TextUtils.isEmpty(videoId)) {
            VideoPlayerDialogFragment.newInstance(videoId)
                    .show(getSupportFragmentManager(), "video_player");
        } else {
            Toast.makeText(this, "Video ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNotes() {
        noteList = new ArrayList<>();
        adapter = new NoteAdapter(noteList);
        notesRecycler.setLayoutManager(new LinearLayoutManager(this));
        notesRecycler.setAdapter(adapter);

        db.collection("SharedNotes")
                .whereEqualTo("videoId", videoId)
                .orderBy("timestamp") // Requires index likely
                .addSnapshotListener((value, error) -> {
                    if (error != null)
                        return;
                    if (value == null)
                        return;

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            noteList.add(dc.getDocument().toObject(NoteModel.class));
                            adapter.notifyItemInserted(noteList.size() - 1);
                            notesRecycler.scrollToPosition(noteList.size() - 1);
                        }
                    }
                });
    }

    private void sendNote() {
        String text = inputNote.getText().toString().trim();
        if (TextUtils.isEmpty(text))
            return;

        if (auth.getCurrentUser() == null)
            return;

        String uid = auth.getCurrentUser().getUid();

        // Fetch User Name first (or store it locally)
        db.collection("Users").document(uid).get().addOnSuccessListener(doc -> {
            String name = "User";
            if (doc.exists() && doc.contains("fullName")) {
                name = doc.getString("fullName");
            }

            String noteId = db.collection("SharedNotes").document().getId();
            long timestamp = System.currentTimeMillis();

            NoteModel note = new NoteModel(noteId, text, uid, name, timestamp);
            // Add videoId for query
            Map<String, Object> data = new HashMap<>();
            data.put("noteId", noteId);
            data.put("text", text);
            data.put("senderId", uid);
            data.put("senderName", name);
            data.put("timestamp", timestamp);
            data.put("videoId", videoId);

            db.collection("SharedNotes").document(noteId).set(data)
                    .addOnSuccessListener(aVoid -> {
                        inputNote.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add note", Toast.LENGTH_SHORT).show());
        });
    }
}
