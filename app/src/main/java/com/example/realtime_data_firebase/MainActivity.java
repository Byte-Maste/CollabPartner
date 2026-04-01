package com.example.realtime_data_firebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.realtime_data_firebase.databinding.ActivityMainBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setupTabs();
        registerUser();
        loginUser();
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Login"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Register"));

        binding.tabLayout
                .addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        if (tab.getPosition() == 0) {
                            binding.loginForm.setVisibility(android.view.View.VISIBLE);
                            binding.registerForm.setVisibility(android.view.View.GONE);
                        } else {
                            binding.loginForm.setVisibility(android.view.View.GONE);
                            binding.registerForm.setVisibility(android.view.View.VISIBLE);
                        }
                    }

                    @Override
                    public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    }
                });
    }

    private void registerUser() {
        binding.btnRegister.setOnClickListener(v -> {

            String fullName = binding.regUserName.getText().toString().trim();
            String email = binding.regEmail.getText().toString().trim();
            String password = binding.regPassword.getText().toString().trim();
            String bio = binding.regBio.getText().toString().trim();

            // Collect checked interests
            List<String> interests = new ArrayList<>();
            for (int i = 0; i < binding.chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) binding.chipGroup.getChildAt(i);
                if (chip.isChecked())
                    interests.add(chip.getText().toString());
            }

            // Validations
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Full name required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (bio.isEmpty()) {
                Toast.makeText(this, "Bio required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (interests.isEmpty()) {
                Toast.makeText(this, "Select at least 1 interest", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(userResult -> {
                        String uid = auth.getCurrentUser().getUid();

                        HashMap<String, Object> userData = new HashMap<>();
                        userData.put("fullName", fullName);
                        userData.put("email", email);
                        userData.put("bio", bio);
                        userData.put("interests", interests);
                        userData.put("streakCount", 1); // Default Streak 1
                        userData.put("lastActiveTime", System.currentTimeMillis());

                        firestore.collection("Users")
                                .document(uid)
                                .set(userData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Registered successfully! Login now", Toast.LENGTH_SHORT)
                                            .show();
                                    binding.tabLayout.getTabAt(0).select();
                                    binding.loginForm.setVisibility(android.view.View.VISIBLE);
                                    binding.registerForm.setVisibility(android.view.View.GONE);
                                })
                                .addOnFailureListener(e -> Toast
                                        .makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show());
                    })
                    .addOnFailureListener(e -> Toast
                            .makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void loginUser() {
        binding.btnLogin.setOnClickListener(v -> {

            String email = binding.loginEmail.getText().toString().trim();
            String password = binding.loginPassword.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, SectionActivity.class));
                        finish();
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private com.google.firebase.firestore.ListenerRegistration callListener;

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            // Status is now handled by MyApplication (ProcessLifecycleOwner)

            // Start listening for calls
            listenForCalls(uid);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Status is now handled by MyApplication (ProcessLifecycleOwner)
    }

    private void listenForCalls(String userId) {
        if (callListener != null)
            callListener.remove();

        callListener = firestore.collection("Calls")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "ringing")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null)
                        return;

                    for (com.google.firebase.firestore.DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            CallModel call = dc.getDocument().toObject(CallModel.class);

                            // Check if call is recent (e.g., within last minute) to avoid old calls
                            if (System.currentTimeMillis() - call.timestamp < 60000) {
                                Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                                intent.putExtra("callId", call.callId);
                                intent.putExtra("callerId", call.callerId);
                                intent.putExtra("callerName", call.callerName);
                                intent.putExtra("callerAvatar", call.callerAvatar);
                                intent.putExtra("roomId", call.roomId);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Important if activity is in
                                                                                // background
                                startActivity(intent);
                            }
                        }
                    }
                });
    }
}
