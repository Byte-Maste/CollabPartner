package com.example.realtime_data_firebase;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

public class OutgoingCallActivity extends AppCompatActivity {

    String receiverId, receiverName, receiverAvatar, roomId, callId;
    ImageView callerImage;
    TextView tvCallerName, tvStatus;
    FloatingActionButton btnEndCall;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ListenerRegistration callListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");
        roomId = getIntent().getStringExtra("roomId");

        callerImage = findViewById(R.id.callerImage);
        tvCallerName = findViewById(R.id.callerName);
        tvStatus = findViewById(R.id.textStatus); // "Waiting for answer..."
        btnEndCall = findViewById(R.id.btnEndCall);

        tvCallerName.setText(receiverName);
        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            Glide.with(this).load(receiverAvatar).circleCrop().into(callerImage);
        }

        btnEndCall.setOnClickListener(v -> endCall());

        initiateCall();
    }

    private void initiateCall() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Fetch current user name/avatar for the call object
        db.collection("Users").document(currentUserId).get().addOnSuccessListener(doc -> {
            String myName = doc.getString("name");
            if (myName == null)
                myName = doc.getString("fullName");
            String myAvatar = doc.getString("profileImageUrl");

            callId = db.collection("Calls").document().getId();

            CallModel call = new CallModel(
                    callId,
                    currentUserId,
                    myName != null ? myName : "User",
                    myAvatar,
                    receiverId,
                    roomId,
                    System.currentTimeMillis());

            db.collection("Calls").document(callId).set(call)
                    .addOnSuccessListener(aVoid -> listenForResponse())
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to place call", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        });
    }

    private void listenForResponse() {
        if (callId == null)
            return;

        callListener = db.collection("Calls").document(callId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists())
                        return;

                    String status = snapshot.getString("status");
                    if ("accepted".equals(status)) {
                        tvStatus.setText("Connected");
                        tvStatus.setTextColor(getResources().getColor(R.color.success)); // Make sure color exists or
                                                                                         // use Color.GREEN

                        // Delay joining slightly to let user see "Connected"
                        new Handler().postDelayed(() -> {
                            joinMeeting();
                            finish(); // Close outgoing screen and open Jitsi
                        }, 1500);
                    } else if ("rejected".equals(status)) {
                        tvStatus.setText("Call Declined");
                        new Handler().postDelayed(this::finish, 2000);
                    }
                });
    }

    private void endCall() {
        if (callId != null) {
            db.collection("Calls").document(callId).update("status", "ended");
        }
        finish();
    }

    private void joinMeeting() {
        try {
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setRoom(roomId)
                    .setFeatureFlag("lobby-mode.enabled", false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("prejoinpage.enabled", false)
                    .build();
            JitsiMeetActivity.launch(this, options);
        } catch (Exception e) {
            Toast.makeText(this, "Error launching meeting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callListener != null)
            callListener.remove();
    }
}
