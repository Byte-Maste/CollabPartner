package com.example.realtime_data_firebase;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import android.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.widget.CheckBox;
import android.view.ViewGroup;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;

public class ChatActivity extends AppCompatActivity {

    RecyclerView recycler;
    EditText inputMsg;
    ImageView btnSend;
    MessageAdapter adapter;
    List<MessageModel> messages = new ArrayList<>();
    FirebaseFirestore db;
    String chatId, myId;
    String otherUserId = null;
    String chatType = "individual";
    String currentChatName = "";
    TextView subtitle;
    Handler typingHandler = new Handler();
    Runnable typingRunnable;
    boolean isTyping = false;
    boolean otherUserTyping = false;
    String lastPresence = "Offline";

    private final androidx.activity.result.ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    android.util.Log.d("DEBUG_CHAT", "File URI received: " + uri.toString());
                    uploadFile(uri);
                } else {
                    android.util.Log.d("DEBUG_CHAT", "File Picker cancelled or URI null");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        findViewById(R.id.back).setOnClickListener(v -> finish());

        // ... (rest of onCreate remains similar, just updating the click listener)

        db = FirebaseFirestore.getInstance();
        myId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatId = getIntent().getStringExtra("chatId");

        // Setup Header
        TextView title = findViewById(R.id.chatTitle);
        subtitle = findViewById(R.id.chatSubtitle);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        recycler = findViewById(R.id.msgRecycler);
        inputMsg = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);

        btnMenu.setOnClickListener(v -> showMenu(v));

        if (getIntent().hasExtra("name")) {
            currentChatName = getIntent().getStringExtra("name");
            title.setText(currentChatName);
        } else if (getIntent().hasExtra("otherUserId")) {
            otherUserId = getIntent().getStringExtra("otherUserId");
            setupPresence(otherUserId);
            db.collection("Users").document(otherUserId).get().addOnSuccessListener(doc -> {
                if (doc.exists())
                    title.setText(doc.getString("name"));
            });
        }

        setupTyping();

        // Listeners for Chat Metadata (typing, name, etc.)
        db.collection("Chats").document(chatId).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists())
                return;
            if (doc.contains("chatName") && doc.getString("chatName") != null)
                title.setText(doc.getString("chatName"));
            if (doc.contains("type"))
                chatType = doc.getString("type");

            // Typing Logic (simplified for brevity in this replace block, asserting it
            // exists in original)
            if (doc.contains("typing")) {
                Map<String, Object> typing = (Map<String, Object>) doc.get("typing");
                boolean someoneTyping = false;
                if (typing != null) {
                    for (Map.Entry<String, Object> entry : typing.entrySet()) {
                        if (!entry.getKey().equals(myId) && (boolean) entry.getValue()) {
                            someoneTyping = true;
                            break;
                        }
                    }
                }
                otherUserTyping = someoneTyping;
                if (someoneTyping) {
                    subtitle.setVisibility(View.VISIBLE);
                    subtitle.setText("Typing...");
                    subtitle.setTextColor(getResources().getColor(R.color.accent_blue));
                } else {
                    if ("group".equals(chatType)) {
                        List<String> p = (List<String>) doc.get("participants");
                        subtitle.setVisibility(View.VISIBLE);
                        subtitle.setText(p != null ? p.size() + " members" : "Group Info");
                        subtitle.setTextColor(getResources().getColor(R.color.textSecondary));
                    } else {
                        subtitle.setVisibility(View.VISIBLE);
                        subtitle.setText(lastPresence);
                        subtitle.setTextColor("Online".equals(lastPresence) ? getResources().getColor(R.color.success)
                                : getResources().getColor(R.color.textSecondary));
                    }
                }
            }
        });

        ImageView btnVideoCall = findViewById(R.id.btnVideoCall);

        btnVideoCall.setOnClickListener(v -> {
            if ("group".equals(chatType)) {
                // For group, maybe just join directly or show a list?
                // For now, let's keep direct join for groups as "Calling" a whole group is
                // complex
                try {
                    org.jitsi.meet.sdk.JitsiMeetConferenceOptions options = new org.jitsi.meet.sdk.JitsiMeetConferenceOptions.Builder()
                            .setRoom("collab_group_" + chatId) // Unique room
                            .setFeatureFlag("lobby-mode.enabled", false)
                            .setFeatureFlag("welcomepage.enabled", false)
                            .setFeatureFlag("prejoinpage.enabled", false)
                            .build();
                    org.jitsi.meet.sdk.JitsiMeetActivity.launch(this, options);
                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                // Individual Chat -> Ring them
                if (otherUserId == null) {
                    Toast.makeText(this, "User info not loaded yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Need receiver avatar/name
                String recName = title.getText().toString(); // Currently displayed name
                // Need to fetch avatar if not stored locally, but we can pass what we have
                // Better to fetch fresh in OutgoingCallActivity or pass from here if we had it.
                // We don't have avatar URL handy in valid scope here easily without fetching.
                // Let OutgoingCallActivity fetch it or pass null.

                android.content.Intent intent = new android.content.Intent(ChatActivity.this,
                        OutgoingCallActivity.class);
                intent.putExtra("receiverId", otherUserId);
                intent.putExtra("receiverName", recName);
                // intent.putExtra("receiverAvatar", ...); // We'll let Activity fetch or load
                // default
                intent.putExtra("roomId", "collab_private_" + chatId);
                startActivity(intent);
            }
        });

        adapter = new MessageAdapter(messages, myId, chatType);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage(inputMsg.getText().toString().trim(), "text", null));

        findViewById(R.id.btnAddFile).setOnClickListener(v -> {
            android.util.Log.d("DEBUG_CHAT", "Plus icon clicked - Launching Picker");
            filePickerLauncher.launch("*/*");
        });
    }

    private void uploadFile(android.net.Uri uri) {
        String fileName = getFileName(uri);
        String mimeType = getContentResolver().getType(uri);
        String type = (mimeType != null && mimeType.startsWith("image/")) ? "image" : "file";

        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setTitle("Uploading...");
        pd.show();

        String storagePath = "chat_files/" + chatId + "/" + System.currentTimeMillis() + "_" + fileName;

        com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage
                .getInstance("gs://realtime-data-firebase-d95c2.firebasestorage.app");
        android.util.Log.d("DEBUG_CHAT", "Storage Bucket: " + storage.getReference().getBucket());

        com.google.firebase.storage.StorageReference ref = storage.getReference().child(storagePath);

        ref.putFile(uri)
                .addOnSuccessListener(task -> {
                    ref.getDownloadUrl().addOnSuccessListener(url -> {
                        pd.dismiss();
                        sendMessage(url.toString(), type, fileName);
                    });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    android.util.Log.e("DEBUG_CHAT", "Upload Failed", e);
                    if (e instanceof com.google.firebase.storage.StorageException) {
                        int errorCode = ((com.google.firebase.storage.StorageException) e).getErrorCode();
                        int httpResultCode = ((com.google.firebase.storage.StorageException) e).getHttpResultCode();
                        android.util.Log.e("DEBUG_CHAT", "Error Code: " + errorCode + ", Http Code: " + httpResultCode);
                    }
                    Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getFileName(android.net.Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1)
                        result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1)
                result = result.substring(cut + 1);
        }
        return result;
    }

    private void sendMessage(String content, String type, String fileName) {
        if (content.isEmpty())
            return;

        String msgId = db.collection("Chats").document(chatId).collection("Messages").document().getId();
        MessageModel msg = new MessageModel(msgId, myId, content, type);
        msg.fileName = fileName; // Manually set if needed or update constructor

        // 1. Save Message
        db.collection("Chats").document(chatId).collection("Messages").document(msgId).set(msg);

        // 2. Update Chat "Last Message"
        Map<String, Object> update = new HashMap<>();
        update.put("lastMessage", type.equals("text") ? content : (type.equals("image") ? "📷 Image" : "📎 File"));
        update.put("lastMessageTime", System.currentTimeMillis());
        db.collection("Chats").document(chatId).update(update);

        if (type.equals("text"))
            inputMsg.setText("");
    }

    private void clearChat() {
        if (chatId == null)
            return;

        // Method 2: Soft Delete
        long clearTime = System.currentTimeMillis();
        db.collection("Chats").document(chatId).collection("Participants").document(myId)
                .update("clearedAt", clearTime)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Chat Cleared", Toast.LENGTH_SHORT).show();
                    // Reload messages: Clear list and re-fetch with new filter
                    messages.clear();
                    adapter.notifyDataSetChanged();
                    loadMessages(); // Will reuse but needs filter (see loadMessages update)
                })
                .addOnFailureListener(e -> {
                    // If document doesn't exist (legacy), set it
                    Map<String, Object> data = new HashMap<>();
                    data.put("clearedAt", clearTime);
                    db.collection("Chats").document(chatId).collection("Participants").document(myId)
                            .set(data, com.google.firebase.firestore.SetOptions.merge());
                    Toast.makeText(this, "Chat Cleared", Toast.LENGTH_SHORT).show();
                    messages.clear();
                    adapter.notifyDataSetChanged();
                    loadMessages();
                });
    }

    private void loadMessages() {
        // First fetch 'clearedAt' timestamp
        db.collection("Chats").document(chatId).collection("Participants").document(myId)
                .get().addOnSuccessListener(doc -> {
                    long clearedAt = 0;
                    if (doc.exists() && doc.contains("clearedAt")) {
                        clearedAt = doc.getLong("clearedAt");
                    }
                    listenForMessages(clearedAt);
                });
    }

    private void listenForMessages(long afterTimestamp) {
        // Stop previous listener if needed (not implemented for simplicity, but good
        // practice)
        // For now, simpler approach: Query filters
        com.google.firebase.firestore.Query query = db.collection("Chats").document(chatId).collection("Messages")
                .orderBy("timestamp");

        if (afterTimestamp > 0) {
            query = query.whereGreaterThan("timestamp", afterTimestamp);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null)
                return;
            if (value == null)
                return;
            for (DocumentChange dc : value.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    messages.add(dc.getDocument().toObject(MessageModel.class));
                    adapter.notifyItemInserted(messages.size() - 1);
                    recycler.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    private void setupTyping() {
        inputMsg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping) {
                    isTyping = true;
                    db.collection("Chats").document(chatId).update("typing." + myId, true);
                }
                typingHandler.removeCallbacks(typingRunnable);
                typingRunnable = () -> {
                    isTyping = false;
                    db.collection("Chats").document(chatId).update("typing." + myId, false);
                };
                typingHandler.postDelayed(typingRunnable, 1500);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupPresence(String uid) {
        db.collection("Users").document(uid).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null)
                return;
            if (otherUserTyping)
                return; // Don't overwrite "Typing..."

            String status = doc.getString("status");
            subtitle.setVisibility(android.view.View.VISIBLE);

            if ("Online".equals(status)) {
                subtitle.setText("Online");
                subtitle.setTextColor(getResources().getColor(R.color.success)); // Make sure success color exists or
                                                                                 // use generic green
            } else {
                long last = doc.contains("lastSeen") ? (long) doc.get("lastSeen") : 0;
                if (last > 0) {
                    // Simple time format
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a");
                    subtitle.setText("Last seen " + sdf.format(new java.util.Date(last)));
                } else {
                    subtitle.setText("Offline");
                }
                subtitle.setTextColor(getResources().getColor(R.color.textSecondary));
            }
        });
    }

    private void showMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        if ("group".equals(chatType)) {
            popup.getMenu().add("Edit Group Name");
            popup.getMenu().add("Add Members");
            popup.getMenu().add("Clear Chat");
        } else {
            popup.getMenu().add("Edit Name");
            popup.getMenu().add("Clear Chat");
        }
        // Add Create Meeting to both
        popup.getMenu().add("Create Meeting");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Edit Group Name")) {
                showEditGroupDialog(currentChatName);
            } else if (item.getTitle().equals("Add Members")) {
                showAddMembersDialog();
            } else if (item.getTitle().equals("Edit Name")) {
                android.widget.Toast.makeText(this, "Edit Name Clicked", android.widget.Toast.LENGTH_SHORT).show();
            } else if (item.getTitle().equals("Clear Chat")) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Clear Chat")
                        .setMessage("Are you sure you want to clear this chat?")
                        .setPositiveButton("Clear", (d, w) -> clearChat())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else if (item.getTitle().equals("Create Meeting")) {
                String room = "collab_meet_" + System.currentTimeMillis();
                String link = "https://meet.jit.si/" + room;
                sendMessage("📞 Join my meeting: " + link, "text", null);
            }
            return true;
        });
        popup.show();
    }

    private void showEditGroupDialog(String currentName) {
        android.app.Dialog d = new android.app.Dialog(this);
        android.view.View v = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        d.setContentView(v);
        if (d.getWindow() != null)
            d.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView head = d.findViewById(R.id.dialogTitle);
        if (head != null)
            head.setText("Edit Group Name");

        EditText input = d.findViewById(R.id.inputGroupName);
        input.setText(currentName);

        RecyclerView rv = d.findViewById(R.id.contactsRecycler);
        rv.setVisibility(android.view.View.GONE);

        android.widget.Button btn = d.findViewById(R.id.btnFinalizeGroup);
        btn.setText("Save Name");

        btn.setOnClickListener(click -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                input.setError("Required");
                return;
            }

            db.collection("Chats").document(chatId).update("chatName", newName)
                    .addOnSuccessListener(u -> {
                        android.widget.Toast.makeText(this, "Group Name Updated", android.widget.Toast.LENGTH_SHORT)
                                .show();
                        d.dismiss();
                    });
        });

        d.show();
    }

    private void showAddMembersDialog() {
        android.app.Dialog d = new android.app.Dialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        d.setContentView(v);
        if (d.getWindow() != null)
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView head = d.findViewById(R.id.dialogTitle);
        if (head != null)
            head.setText("Add Members");

        EditText input = d.findViewById(R.id.inputGroupName);
        input.setVisibility(View.GONE); // Hide Name Input

        RecyclerView rv = d.findViewById(R.id.contactsRecycler);
        Button btn = d.findViewById(R.id.btnFinalizeGroup);
        btn.setText("Add Selected");

        List<CollabModel> contacts = new ArrayList<>();
        List<String> selectedIds = new ArrayList<>();
        ContactSelectAdapter contactAdapter = new ContactSelectAdapter(contacts, selectedIds);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(contactAdapter);

        // Fetch current participants to exclude them
        db.collection("Chats").document(chatId).get().addOnSuccessListener(chatDoc -> {
            List<String> currentParticipants = (List<String>) chatDoc.get("participants");
            if (currentParticipants == null)
                currentParticipants = new ArrayList<>();
            final List<String> finalCurrent = currentParticipants;

            // INNOVATION: Filter only Connected Users (Friends)
            // 1. Get all "private" chats I am in
            db.collection("Chats")
                    .whereArrayContains("participants", myId)
                    .whereEqualTo("type", "private") // Only friends
                    .get()
                    .addOnSuccessListener(chats -> {
                        List<String> friendIds = new ArrayList<>();
                        for (DocumentSnapshot c : chats) {
                            List<String> parts = (List<String>) c.get("participants");
                            if (parts != null) {
                                for (String id : parts) {
                                    if (!id.equals(myId))
                                        friendIds.add(id);
                                }
                            }
                        }

                        // 2. Fetch User Details for these IDs
                        if (friendIds.isEmpty()) {
                            Toast.makeText(this, "No connected friends found", Toast.LENGTH_SHORT).show();
                            d.dismiss();
                            return;
                        }

                        // Firestore 'in' query supports up to 10. For scalability in this demo, we can
                        // just fetch all users and filter in memory
                        // OR (Better) iterate and fetch. For simplicity/speed in demo with few users:
                        // fetch all and filter.

                        db.collection("Users").get().addOnSuccessListener(snaps -> {
                            contacts.clear();
                            for (DocumentSnapshot doc : snaps) {
                                // Conditions:
                                // 1. Must be in friendIds (Connected)
                                // 2. Must NOT be in currentParticipants (Already in group)
                                if (friendIds.contains(doc.getId()) && !finalCurrent.contains(doc.getId())) {
                                    contacts.add(new CollabModel(doc.getId(), doc.getString("name"), "", "", ""));
                                }
                            }

                            // Check if any friends are convertible to group members
                            if (contacts.isEmpty()) {
                                Toast.makeText(this, "All friends are already in this group", Toast.LENGTH_SHORT)
                                        .show();
                                d.dismiss();
                            } else {
                                contactAdapter.notifyDataSetChanged();
                            }
                        });
                    });
        });

        btn.setOnClickListener(click -> {
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Select at least 1 user", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Chats").document(chatId).update("participants", FieldValue.arrayUnion(selectedIds.toArray()))
                    .addOnSuccessListener(u -> {
                        Toast.makeText(this, "Members Added", Toast.LENGTH_SHORT).show();
                        d.dismiss();
                    });
        });

        d.show();
    }

    // Reuse Adapter Logic
    static class ContactSelectAdapter extends RecyclerView.Adapter<ContactSelectAdapter.Holder> {
        List<CollabModel> list;
        List<String> selectedIds;

        public ContactSelectAdapter(List<CollabModel> list, List<String> selectedIds) {
            this.list = list;
            this.selectedIds = selectedIds;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CheckBox cb = new CheckBox(parent.getContext());
            cb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Holder(cb);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int i) {
            CollabModel u = list.get(i);
            h.cb.setText(u.name);
            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(selectedIds.contains(u.userId));

            h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked)
                    selectedIds.add(u.userId);
                else
                    selectedIds.remove(u.userId);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            CheckBox cb;

            public Holder(View v) {
                super(v);
                cb = (CheckBox) v;
            }
        }
    }
}