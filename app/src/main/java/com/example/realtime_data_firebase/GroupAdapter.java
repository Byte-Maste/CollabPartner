package com.example.realtime_data_firebase;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupHolder> {

    List<ChatModel> list;
    String myId;
    OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatModel chat);
    }

    public GroupAdapter(List<ChatModel> list, String myId, OnChatClickListener listener) {
        this.list = list;
        this.myId = myId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupHolder h, int i) {
        ChatModel c = list.get(i);

        // Logic to determine name
        if ("group".equals(c.type)) {
            h.name.setText(c.chatName);
            h.iconText.setText(String.valueOf(c.chatName.charAt(0)));
        } else {
            // Private chat: Fetch the other user's name
            String friendId = null;
            if (c.participants != null) {
                for (String uid : c.participants) {
                    if (!uid.equals(myId)) {
                        friendId = uid;
                        break;
                    }
                }
            }

            if (friendId != null) {
                // Fetch friend's name and image from Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("Users")
                        .document(friendId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("name");
                                if (name == null)
                                    name = doc.getString("fullName");
                                String imageUrl = doc.getString("profileImageUrl");

                                if (name != null) {
                                    h.name.setText(name);
                                    h.iconText.setText(String.valueOf(name.charAt(0)).toUpperCase());
                                } else {
                                    h.name.setText("User");
                                    h.iconText.setText("U");
                                }

                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    h.iconText.setVisibility(View.GONE);
                                    h.chatIcon.setVisibility(View.VISIBLE);
                                    Glide.with(h.itemView.getContext())
                                            .load(imageUrl)
                                            .circleCrop()
                                            .into(h.chatIcon);
                                } else {
                                    h.iconText.setVisibility(View.VISIBLE);
                                    h.chatIcon.setVisibility(View.GONE);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            h.name.setText("Private Chat");
                            h.iconText.setVisibility(View.VISIBLE);
                            h.chatIcon.setVisibility(View.GONE);
                        });
            } else {
                h.name.setText("Private Chat");
                h.iconText.setVisibility(View.VISIBLE);
                h.chatIcon.setVisibility(View.GONE);
            }
        }

        h.msg.setText(c.lastMessage);

        // Format Time
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        h.time.setText(sdf.format(new Date(c.lastMessageTime)));

        h.itemView.setOnClickListener(v -> listener.onChatClick(c));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class GroupHolder extends RecyclerView.ViewHolder {
        TextView name, msg, time, iconText;
        ImageView chatIcon;

        public GroupHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chatName);
            msg = itemView.findViewById(R.id.lastMessage);
            time = itemView.findViewById(R.id.msgTime);
            iconText = itemView.findViewById(R.id.chatIconText);
            chatIcon = itemView.findViewById(R.id.chatIcon);
        }
    }
}