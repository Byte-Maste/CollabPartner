package com.example.realtime_data_firebase;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder> {

    List<MessageModel> list;
    String myId;
    String chatType; // Added chatType

    public MessageAdapter(List<MessageModel> list, String myId, String chatType) {
        this.list = list;
        this.myId = myId;
        this.chatType = chatType;
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder h, int i) {
        MessageModel m = list.get(i);

        // Reset Visibility
        h.text.setVisibility(View.VISIBLE);
        h.image.setVisibility(View.GONE);
        h.filePanel.setVisibility(View.GONE);
        h.senderName.setVisibility(View.GONE);
        h.profileCard.setVisibility(View.GONE); // Default GONE

        // SENDER NAME LOGIC (Only for Group Chat and NOT my message)
        if ("group".equals(chatType) && !m.senderId.equals(myId)) {
            h.senderName.setVisibility(View.VISIBLE);
            // Fetch name from Firestore based on senderId (Optimized: Should pass a Map of
            // names or fetch once)
            // For now, simple fetch (inefficient but works for prototype) or show ID if
            // name not available locally
            // Ideally, ChatActivity should pass a Map<String, User> of participants.
            // Using a placeholder or simple fetch here might cause flickering.
            // Let's try to fetch if not cached, or better:
            // For this iteration, we will just display a placeholder or fetch freshly.
            // A better approach: The MessageModel should ideally have senderName, or we
            // look it up.
            // Let's generic fetch for now:
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("Users").document(m.senderId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            h.senderName.setText(doc.getString("username"));

                            // Load profile image
                            String imageUrl = doc.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                h.profileCard.setVisibility(View.VISIBLE);
                                com.bumptech.glide.Glide.with(h.itemView.getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_person)
                                        .into(h.profileImage);
                            } else {
                                h.profileCard.setVisibility(View.VISIBLE);
                                h.profileImage.setImageResource(R.drawable.ic_person);
                            }
                        }
                    });
        }

        // TIME LOGIC
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
        h.time.setText(sdf.format(new java.util.Date(m.timestamp)));

        // ALIGNMENT LOGIC
        if (m.senderId.equals(myId)) {
            h.root.setGravity(Gravity.END);
            h.container.setBackgroundResource(R.drawable.bubble_right);
            h.text.setTextColor(h.itemView.getResources().getColor(android.R.color.white));
            h.time.setTextColor(h.itemView.getResources().getColor(android.R.color.white)); // White time for my msg
            h.senderName.setVisibility(View.GONE); // Never show my name
            h.profileCard.setVisibility(View.GONE); // Never show my own profile pic next to my msg
        } else {
            h.root.setGravity(Gravity.START);
            h.container.setBackgroundResource(R.drawable.bubble_left);
            h.text.setTextColor(h.itemView.getResources().getColor(R.color.text));
            h.time.setTextColor(h.itemView.getResources().getColor(R.color.text)); // Dark time for others

            // Show profile icon for others in BOTH private and group chats
            h.profileCard.setVisibility(View.VISIBLE);

            // Re-fetch or load if not already handled in group logic
            if (!"group".equals(chatType)) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("Users").document(m.senderId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String imageUrl = doc.getString("profileImageUrl");
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    com.bumptech.glide.Glide.with(h.itemView.getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_person)
                                            .into(h.profileImage);
                                } else {
                                    h.profileImage.setImageResource(R.drawable.ic_person);
                                }
                            }
                        });
            }
        }

        // CONTENT LOGIC
        if ("image".equals(m.type)) {
            h.image.setVisibility(View.VISIBLE);
            h.text.setVisibility(View.GONE);
            com.bumptech.glide.Glide.with(h.itemView.getContext()).load(m.text).into(h.image);
            h.image.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(android.net.Uri.parse(m.text), "image/*");
                h.itemView.getContext().startActivity(intent);
            });
        } else if ("file".equals(m.type)) {
            h.filePanel.setVisibility(View.VISIBLE);
            h.text.setVisibility(View.GONE);
            h.fileName.setText(m.fileName != null ? m.fileName : "File");
            h.filePanel.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(android.net.Uri.parse(m.text), "*/*");
                h.itemView.getContext().startActivity(intent);
            });
        } else {
            h.text.setText(m.text);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MessageHolder extends RecyclerView.ViewHolder {
        TextView text, fileName, senderName, time;
        LinearLayout root, container, filePanel;
        android.widget.ImageView image, profileImage;
        androidx.cardview.widget.CardView profileCard;

        public MessageHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.msgText);
            root = itemView.findViewById(R.id.msgRoot);
            container = itemView.findViewById(R.id.msgContainer);
            image = itemView.findViewById(R.id.msgImage);
            filePanel = itemView.findViewById(R.id.msgFilePanel);
            fileName = itemView.findViewById(R.id.msgFileName);
            senderName = itemView.findViewById(R.id.txtSenderName);
            time = itemView.findViewById(R.id.txtTime);
            profileCard = itemView.findViewById(R.id.msgProfileCard);
            profileImage = itemView.findViewById(R.id.msgProfileImage);
        }
    }
}