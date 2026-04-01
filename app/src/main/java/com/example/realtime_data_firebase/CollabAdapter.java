package com.example.realtime_data_firebase;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CollabAdapter extends RecyclerView.Adapter<CollabAdapter.CollabHolder> {

    List<CollabModel> fullList; // Keeps all data
    List<CollabModel> filterList; // Keeps search results

    public CollabAdapter(List<CollabModel> list) {
        this.fullList = list;
        this.filterList = new ArrayList<>(list); // Initialize with full list
    }

    // Method to filter data
    public void filter(String text) {
        filterList.clear();
        if(text.isEmpty()){
            filterList.addAll(fullList);
        } else{
            text = text.toLowerCase();
            for(CollabModel item: fullList){
                if(item.name.toLowerCase().contains(text) ||
                        (item.headline != null && item.headline.toLowerCase().contains(text))){
                    filterList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Call this when Firestore loads data
    public void updateData(List<CollabModel> newList) {
        fullList.clear();
        fullList.addAll(newList);
        filter(""); // Reset filter
    }

    @NonNull
    @Override
    public CollabHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collab, parent, false);
        return new CollabHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CollabHolder h, int i) {
        CollabModel p = filterList.get(i);

        h.name.setText(p.name);
        h.headline.setText(p.headline != null ? p.headline : "No headline");
        h.location.setText(p.location != null ? p.location : "Remote");
        h.about.setText(p.about != null ? p.about : "No bio available");

        // --- UPDATED SEND LOGIC ---
        h.btnConnect.setOnClickListener(v -> {
            h.btnConnect.setText("Sending...");
            h.btnConnect.setEnabled(false);

            // Get Current User Data to send along with request
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            String myId = auth.getCurrentUser().getUid();

            // 1. Fetch MY details first to send to the other user
            db.collection("Users").document(myId).get().addOnSuccessListener(doc -> {
                String myName = doc.getString("name");
                String myHead = doc.getString("headline");

                // 2. Create Request Object
                String requestId = db.collection("Requests").document().getId();
                RequestModel req = new RequestModel(requestId, myId, myName, myHead, p.userId);

                // 3. Save to "Requests" Collection
                db.collection("Requests").document(requestId).set(req)
                        .addOnSuccessListener(unused -> {
                            h.btnConnect.setText("Sent");
                            h.btnConnect.setBackgroundColor(Color.GRAY);
                            Toast.makeText(h.itemView.getContext(), "Request Sent!", Toast.LENGTH_SHORT).show();
                        });
            });
        });
    }

    @Override
    public int getItemCount() {
        return filterList.size();
    }

    class CollabHolder extends RecyclerView.ViewHolder {
        TextView name, headline, location, about;
        Button btnConnect;

        public CollabHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.collabName);
            headline = itemView.findViewById(R.id.collabHeadline);
            location = itemView.findViewById(R.id.collabLocation);
            about = itemView.findViewById(R.id.collabAbout);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}