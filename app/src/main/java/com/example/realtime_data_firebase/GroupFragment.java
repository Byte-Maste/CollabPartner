package com.example.realtime_data_firebase;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

public class GroupFragment extends Fragment {

    RecyclerView recyclerView;
    GroupAdapter adapter;
    List<ChatModel> chatList = new ArrayList<>();
    FirebaseFirestore db;
    String myId;

    public GroupFragment() { super(R.layout.fragment_group); }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        myId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.groupRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GroupAdapter(chatList, myId, this::openChatActivity);
        recyclerView.setAdapter(adapter);

        loadChats();

        view.findViewById(R.id.btnCreateGroup).setOnClickListener(v -> showCreateGroupDialog());
    }

    private void loadChats() {
        db.collection("Chats")
                .whereArrayContains("participants", myId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    chatList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        chatList.add(doc.toObject(ChatModel.class));
                    }
                    adapter.notifyDataSetChanged(); // Error Resolved: Method exists in Adapter
                });
    }

    private void openChatActivity(ChatModel chat) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("chatId", chat.chatId);
        intent.putExtra("type", chat.type);
        if ("group".equals(chat.type)) {
            intent.putExtra("name", chat.chatName);
        } else {
            String otherId = chat.participants.get(0).equals(myId) ? chat.participants.get(1) : chat.participants.get(0);
            intent.putExtra("otherUserId", otherId);
        }
        startActivity(intent);
    }

    // --- FULL CREATE GROUP DIALOG IMPLEMENTATION ---
    private void showCreateGroupDialog() {
        Dialog dialog = new Dialog(getContext());
        View d = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_group, null);
        dialog.setContentView(d);
        if(dialog.getWindow() != null) dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText groupNameInput = d.findViewById(R.id.inputGroupName);
        RecyclerView contactsRecycler = d.findViewById(R.id.contactsRecycler);
        Button btnCreate = d.findViewById(R.id.btnFinalizeGroup);

        // Setup Selection List
        List<CollabModel> contacts = new ArrayList<>();
        List<String> selectedIds = new ArrayList<>();
        ContactSelectAdapter contactAdapter = new ContactSelectAdapter(contacts, selectedIds); // Internal Class below
        contactsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsRecycler.setAdapter(contactAdapter);

        // Load Users (Ideally only friends, but loading all users for demo)
        db.collection("Users").get().addOnSuccessListener(snaps -> {
            for(DocumentSnapshot doc : snaps) {
                if(!doc.getId().equals(myId)) { // Don't add myself
                    contacts.add(new CollabModel(doc.getId(), doc.getString("name"), "","", ""));
                }
            }
            contactAdapter.notifyDataSetChanged();
        });

        btnCreate.setOnClickListener(v -> {
            String gName = groupNameInput.getText().toString().trim();
            if(gName.isEmpty()) { groupNameInput.setError("Required"); return; }
            if(selectedIds.isEmpty()) { Toast.makeText(getContext(), "Select at least 1 member", Toast.LENGTH_SHORT).show(); return; }

            // Create Group Logic
            String chatId = db.collection("Chats").document().getId();
            selectedIds.add(myId); // Add admin (me)

            ChatModel newGroup = new ChatModel(chatId, selectedIds, "group", gName);
            db.collection("Chats").document(chatId).set(newGroup).addOnSuccessListener(u -> {
                Toast.makeText(getContext(), "Group Created!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // --- INTERNAL ADAPTER FOR SELECTING CONTACTS IN DIALOG ---
    static class ContactSelectAdapter extends RecyclerView.Adapter<ContactSelectAdapter.Holder> {
        List<CollabModel> list;
        List<String> selectedIds;

        public ContactSelectAdapter(List<CollabModel> list, List<String> selectedIds) {
            this.list = list;
            this.selectedIds = selectedIds;
        }
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Reusing a simple checkbox layout logic programmatically or simple view
            CheckBox cb = new CheckBox(parent.getContext());
            cb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Holder(cb);
        }
        @Override
        public void onBindViewHolder(@NonNull Holder h, int i) {
            CollabModel u = list.get(i);
            h.cb.setText(u.name);
            h.cb.setOnCheckedChangeListener(null); // Reset listener
            h.cb.setChecked(selectedIds.contains(u.userId));

            h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked) selectedIds.add(u.userId);
                else selectedIds.remove(u.userId);
            });
        }
        @Override
        public int getItemCount() { return list.size(); }
        static class Holder extends RecyclerView.ViewHolder { CheckBox cb; public Holder(View v) { super(v); cb = (CheckBox)v; } }
    }
}