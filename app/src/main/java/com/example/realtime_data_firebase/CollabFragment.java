package com.example.realtime_data_firebase;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CollabFragment extends Fragment {

    RecyclerView recyclerView;
    EditText searchBox;
    CollabAdapter adapter;
    List<CollabModel> userList = new ArrayList<>();
    FirebaseFirestore firestore;
    FirebaseAuth auth;

    public CollabFragment() { super(R.layout.fragment_collab); }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.collabRecycler);
        searchBox = view.findViewById(R.id.searchBox);

        // Initialize Adapter
        adapter = new CollabAdapter(userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Badge Listener (Updates the Red Counter)
        TextView badge = view.findViewById(R.id.requestCountBadge);
        String myId = auth.getCurrentUser().getUid();

        firestore.collection("Requests")
                .whereEqualTo("receiverId", myId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    int count = value.size();
                    if (count > 0) {
                        badge.setText(String.valueOf(count));
                        badge.setVisibility(View.VISIBLE);
                    } else {
                        badge.setVisibility(View.GONE);
                    }
                });

        // Click Listener on the FRAME (Icon + Badge area)
        view.findViewById(R.id.btnRequestsFrame).setOnClickListener(v -> showRequestsDialog());

        // --- ERROR WAS HERE: These lines must be INSIDE onViewCreated ---
        loadPotentialConnections();
        setupSearch();
    }

    private void showRequestsDialog() {
        Dialog dialog = new Dialog(getContext());
        View d = getLayoutInflater().inflate(R.layout.dialog_requests, null);
        dialog.setContentView(d);

        if(dialog.getWindow() != null)
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RecyclerView reqRecycler = d.findViewById(R.id.requestsRecycler);
        reqRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        List<RequestModel> reqList = new ArrayList<>();
        RequestAdapter reqAdapter = new RequestAdapter(reqList);
        reqRecycler.setAdapter(reqAdapter);

        // FETCH REQUESTS FOR ME
        String myId = auth.getCurrentUser().getUid();
        firestore.collection("Requests")
                .whereEqualTo("receiverId", myId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snaps -> {
                    for(DocumentSnapshot doc : snaps) {
                        reqList.add(doc.toObject(RequestModel.class));
                    }
                    reqAdapter.notifyDataSetChanged();

                    if(reqList.isEmpty()) {
                        Toast.makeText(getContext(), "No pending requests", Toast.LENGTH_SHORT).show();
                    }
                });

        dialog.show();
    }

    private void loadPotentialConnections() {
        String myId = auth.getCurrentUser().getUid();

        // Log that we are starting
        Log.d("DEBUG_COLLAB", "Current User ID: " + myId);
        Log.d("DEBUG_COLLAB", "Starting to fetch users...");

        firestore.collection("Users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CollabModel> loadedUsers = new ArrayList<>();

                    // Log how many documents were found total
                    Log.d("DEBUG_COLLAB", "Documents found: " + queryDocumentSnapshots.size());

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Log the ID of the document currently being checked
                        Log.d("DEBUG_COLLAB", "Checking User: " + doc.getId());

                        // Skip myself
                        if (doc.getId().equals(myId)) {
                            Log.d("DEBUG_COLLAB", "Skipping myself");
                            continue;
                        }

                        String name = doc.getString("name");
                        String headline = doc.getString("headline");
                        String location = doc.getString("location");
                        String about = doc.getString("about");

                        // CHECK IF NAME IS VALID
                        if (name != null && !name.isEmpty()) {
                            loadedUsers.add(new CollabModel(doc.getId(), name, headline, location, about));
                            Log.d("DEBUG_COLLAB", "Added user: " + name);
                        } else {
                            Log.d("DEBUG_COLLAB", "Skipped user " + doc.getId() + " because name is null or empty");
                        }
                    }

                    // Log final list size
                    Log.d("DEBUG_COLLAB", "Final List Size: " + loadedUsers.size());

                    adapter.updateData(loadedUsers);
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG_COLLAB", "Error fetching users: " + e.getMessage());
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // INNOVATION: Real-time filtering
    private void setupSearch() {
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}