package com.example.realtime_data_firebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestHolder> {

    List<RequestModel> list;

    public RequestAdapter(List<RequestModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public RequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestHolder h, int i) {
        RequestModel r = list.get(i);
        h.name.setText(r.senderName);
        h.headline.setText(r.senderHeadline);

        // ACCEPT LOGIC
        h.accept.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1. Update Request Status
            db.collection("Requests").document(r.requestId).update("status", "accepted");

            // 2. INTELLIGENT FLOW: Create a Chat Room immediately
            String chatId = generateChatId(r.senderId, r.receiverId); // Helper method below

            // Check if chat already exists to avoid duplicates
            db.collection("Chats").document(chatId).get().addOnSuccessListener(chatDoc -> {
                if (!chatDoc.exists()) {
                    List<String> participants = java.util.Arrays.asList(r.senderId, r.receiverId);
                    ChatModel newChat = new ChatModel(chatId, participants, "private", null);

                    db.collection("Chats").document(chatId).set(newChat);
                }
            });

            // 3. UI Updates
            list.remove(i);
            notifyItemRemoved(i);
            Toast.makeText(h.itemView.getContext(), "Connected! You can now chat.", Toast.LENGTH_SHORT).show();
        });



        // DECLINE LOGIC
        h.decline.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("Requests").document(r.requestId).delete();
            list.remove(i);
            notifyItemRemoved(i);
        });
    }

    // Helper to ensure unique ID for 1-on-1 chats regardless of who started it
    private String generateChatId(String u1, String u2) {
        if (u1.compareTo(u2) < 0) return u1 + "_" + u2;
        else return u2 + "_" + u1;
    }
    @Override
    public int getItemCount() {
        return list.size();
    }

    class RequestHolder extends RecyclerView.ViewHolder {
        TextView name, headline;
        ImageView accept, decline;

        public RequestHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.reqName);
            headline = itemView.findViewById(R.id.reqHeadline);
            accept = itemView.findViewById(R.id.btnAccept);
            decline = itemView.findViewById(R.id.btnDecline);
        }
    }
}