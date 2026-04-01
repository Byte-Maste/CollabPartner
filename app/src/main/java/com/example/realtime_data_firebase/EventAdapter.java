package com.example.realtime_data_firebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventHolder> {

    List<EventModel> list;
    OnDeleteListener listener;

    String currentUserId;

    // Interface for delete callback
    public interface OnDeleteListener {
        void onDelete(EventModel event);
    }

    public EventAdapter(List<EventModel> list, String currentUserId, OnDeleteListener listener) {
        this.list = list;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventHolder h, int i) {
        EventModel p = list.get(i);

        h.title.setText(p.title);
        h.desc.setText(p.description);
        h.date.setText(p.date);
        h.time.setText(p.time);
        h.location.setText(p.location);

        // Reset visibility first (recycler view creates view recycling issues
        // otherwise)
        h.btnDelete.setVisibility(View.GONE);

        // ATTEND LOGIC
        boolean isAttending = p.attendees != null && p.attendees.contains(currentUserId);
        android.widget.Button btnAttend = h.itemView.findViewById(R.id.btnAttendEvent);

        if (isAttending) {
            btnAttend.setText("Registered");
            btnAttend.setEnabled(false);
            btnAttend.setAlpha(0.5f);
        } else {
            btnAttend.setText("Attend Event");
            btnAttend.setEnabled(true);
            btnAttend.setAlpha(1.0f);

            btnAttend.setOnClickListener(v -> {
                btnAttend.setEnabled(false); // Prevent double click
                btnAttend.setText("Registering...");

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("Events").document(p.eventId)
                        .update("attendees", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                        .addOnSuccessListener(a -> {
                            // Email Trigger
                            sendEmailRequest(p, h.itemView.getContext());
                            android.widget.Toast.makeText(h.itemView.getContext(), "You are now attending!",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            // UI will update automatically via real-time listener in Fragment
                        })
                        .addOnFailureListener(e -> {
                            btnAttend.setEnabled(true);
                            btnAttend.setText("Attend Event");
                            android.widget.Toast.makeText(h.itemView.getContext(), "Failed: " + e.getMessage(),
                                    android.widget.Toast.LENGTH_SHORT).show();
                        });
            });
        }

        // ONLY allow Long Click if the current user is the CREATOR
        if (p.creatorId != null && p.creatorId.equals(currentUserId)) {

            h.itemView.setOnLongClickListener(v -> {
                h.btnDelete.setVisibility(View.VISIBLE);
                return true;
            });

            h.btnDelete.setOnClickListener(v -> listener.onDelete(p));

        } else {
            // If not the creator, disable long click
            h.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class EventHolder extends RecyclerView.ViewHolder {
        TextView title, desc, date, time, location;
        ImageView btnDelete;

        public EventHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.eventTitle);
            desc = itemView.findViewById(R.id.eventDesc);
            time = itemView.findViewById(R.id.eventTime);
            date = itemView.findViewById(R.id.eventDate);
            location = itemView.findViewById(R.id.eventLocation);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private void sendEmailRequest(EventModel event, android.content.Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null)
            return;

        String userEmail = auth.getCurrentUser().getEmail();
        String userId = auth.getCurrentUser().getUid();

        // If user doesn't have email in auth, try to fetch from Firestore
        if (userEmail == null || userEmail.isEmpty()) {
            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String email = doc.getString("email");
                            if (email != null && !email.isEmpty()) {
                                sendEmailViaJavaMail(event, email, context);
                            }
                        }
                    });
        } else {
            sendEmailViaJavaMail(event, userEmail, context);
        }
    }

    /**
     * Send event details via JavaMail
     */
    private void sendEmailViaJavaMail(EventModel event, String userEmail, android.content.Context context) {
        // Format date and time
        String dateTime = event.date + " at " + event.time;

        // Add meeting link for online events
        String location = event.location;
        if ("Online".equals(event.type) && event.meetingLink != null && !event.meetingLink.isEmpty()) {
            location = event.location + " (Meeting Link: " + event.meetingLink + ")";
        }

        // Send email using EmailService
        EmailService.sendEventEmail(
                context,
                userEmail,
                event.title,
                event.description != null ? event.description : "No description provided",
                location,
                dateTime);
    }
}
