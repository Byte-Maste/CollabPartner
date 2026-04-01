package com.example.realtime_data_firebase;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.realtime_data_firebase.databinding.FragmentEventBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class EventFragment extends Fragment {

    FragmentEventBinding binding;
    FirebaseFirestore firestore;
    FirebaseAuth auth;
    List<EventModel> eventList = new ArrayList<>();
    EventAdapter adapter;

    public EventFragment() {
        super(R.layout.fragment_event);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding = FragmentEventBinding.bind(view);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get the Current User ID
        String currentUid = auth.getCurrentUser().getUid();

        adapter = new EventAdapter(eventList, currentUid, event -> deleteEvent(event));

        binding.eventRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.eventRecycler.setAdapter(adapter);

        loadEvents();

        RadioGroup rg = binding.getRoot().findViewById(R.id.filterRadioGroup);
        rg.setOnCheckedChangeListener((group, checkedId) -> applyFilter());

        binding.btnNewEvent.setOnClickListener(v -> openCreateDialog());
    }

    private void deleteEvent(EventModel event) {
        // DELETE FROM "Events" (Global Collection)
        firestore.collection("Events")
                .document(event.eventId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog(getContext());
        View d = LayoutInflater.from(getContext()).inflate(R.layout.event_dialog, null);
        dialog.setContentView(d);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: Removes default
                                                                                           // white borders
        }
        // close logic for NEWPROJECT Dialog Box
        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Initialize new views
        RadioGroup rgType = d.findViewById(R.id.radioGroupType);
        EditText inputLink = d.findViewById(R.id.inputLink);

        // Show/Hide Link input based on selection
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOnline) {
                inputLink.setVisibility(View.VISIBLE);
            } else {
                inputLink.setVisibility(View.GONE);
            }
        });

        EditText title = d.findViewById(R.id.inputEventTitle);
        EditText desc = d.findViewById(R.id.inputEventDesc);
        EditText date = d.findViewById(R.id.inputDate);
        EditText time = d.findViewById(R.id.inputTime);
        EditText location = d.findViewById(R.id.inputLocation);
        Button create = d.findViewById(R.id.btnCreateEvent);

        create.setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            String ds = desc.getText().toString().trim();
            String da = date.getText().toString().trim();
            String te = time.getText().toString().trim();
            String lc = location.getText().toString().trim();

            // Get Type and Link
            String type = (rgType.getCheckedRadioButtonId() == R.id.rbOnline) ? "Online" : "Offline";
            String link = inputLink.getText().toString().trim();

            if (type.equals("Online") && link.isEmpty()) {
                inputLink.setError("Link Required");
                return;
            }
            if (t.isEmpty()) {
                title.setError("Required");
                return;
            }
            if (ds.isEmpty()) {
                desc.setError("Required");
                return;
            }
            if (da.isEmpty()) {
                date.setError("Reguired");
                return;
            }
            if (te.isEmpty()) {
                time.setError("Reguired");
                return;
            }
            if (lc.isEmpty()) {
                location.setError("Reguired");
                return;
            }
            saveEvent(t, ds, da, te, lc, type, link, dialog);
        });

        dialog.show();
    }

    private void saveEvent(String title, String desc, String date, String time, String location, String type,
            String link, Dialog dialog) {
        String uid = auth.getCurrentUser().getUid();
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);

        data.put("description", desc);
        data.put("date", date);
        data.put("time", time);
        data.put("location", location);
        data.put("eventId", eventId);
        data.put("creatorId", uid); // <--- IMPORTANT: Save the creator's ID
        data.put("type", type);
        data.put("meetingLink", link);
        data.put("createdAt", FieldValue.serverTimestamp());

        // SAVE TO "Events" (Global Collection), NOT "Users"
        firestore.collection("Events")
                .document(eventId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // No need to call loadEvents() if you have a SnapshotListener attached
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private List<EventModel> masterList = new ArrayList<>();

    private void loadEvents() {
        // READ FROM "Events" (Global Collection)
        firestore.collection("Events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null)
                        return;
                    masterList.clear();
                    // Date Parser
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
                    Date today = new Date();
                    // Reset time part of today for accurate comparison
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    today = cal.getTime();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        EventModel m = doc.toObject(EventModel.class);

                        // Check Expiry
                        try {
                            Date eventDate = sdf.parse(m.date);
                            // If eventDate is before today, skip it (Hide expired)
                            if (eventDate != null && eventDate.before(today)) {
                                continue;
                            }
                        } catch (Exception e) {
                            // If date parse fails, show it or hide it? Let's show it to be safe or log
                            // error.
                        }

                        masterList.add(m);
                    }
                    applyFilter();
                });
    }

    private void applyFilter() {
        RadioGroup rg = binding.getRoot().findViewById(R.id.filterRadioGroup);
        boolean showMyEvents = rg.getCheckedRadioButtonId() == R.id.radioMy;
        String myId = auth.getCurrentUser().getUid();

        eventList.clear();
        if (showMyEvents) {
            for (EventModel m : masterList) {
                if (m.attendees != null && m.attendees.contains(myId)) {
                    eventList.add(m);
                }
            }
        } else {
            eventList.addAll(masterList);
        }
        adapter.notifyDataSetChanged();
    }
}
