package com.example.realtime_data_firebase;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

public class IncomingCallActivity extends AppCompatActivity {

    String callId, callerId, callerName, callerAvatar, roomId;
    ImageView callerImage;
    TextView tvCallerName;
    View btnAccept, btnDecline;
    FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        db = FirebaseFirestore.getInstance();

        callId = getIntent().getStringExtra("callId");
        callerId = getIntent().getStringExtra("callerId");
        callerName = getIntent().getStringExtra("callerName");
        callerAvatar = getIntent().getStringExtra("callerAvatar");
        roomId = getIntent().getStringExtra("roomId");

        callerImage = findViewById(R.id.callerImage);
        tvCallerName = findViewById(R.id.callerName);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        tvCallerName.setText(callerName);

        if (callerAvatar != null && !callerAvatar.isEmpty()) {
            Glide.with(this).load(callerAvatar).circleCrop().into(callerImage);
        }

        btnAccept.setOnClickListener(v -> acceptCall());
        btnDecline.setOnClickListener(v -> declineCall());
    }

    private void acceptCall() {
        // Update status to accepted
        db.collection("Calls").document(callId).update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    // Show "Connected" UI
                    TextView statusText = findViewById(R.id.textStatus); // "Incoming Video Call..."
                    if (statusText != null) {
                        statusText.setText("Connected");
                        statusText.setTextColor(getResources().getColor(R.color.success));
                    }

                    // Hide buttons
                    btnAccept.setVisibility(View.INVISIBLE);
                    btnDecline.setVisibility(View.INVISIBLE);

                    new android.os.Handler().postDelayed(() -> {
                        joinMeeting();
                        finish();
                    }, 1000);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error accepting call", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void declineCall() {
        // Update status to rejected
        db.collection("Calls").document(callId).update("status", "rejected")
                .addOnSuccessListener(aVoid -> finish());
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
}
