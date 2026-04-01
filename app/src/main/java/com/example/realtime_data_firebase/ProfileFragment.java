package com.example.realtime_data_firebase;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.realtime_data_firebase.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    FragmentProfileBinding binding;
    FirebaseFirestore firestore;
    FirebaseAuth auth;
    ProfileModel currentProfile;
    ListenerRegistration profileListener;
    FirebaseStorage storage;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadProfileImage(uri);
                }
            });

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.bind(view);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // 1. Start listening to changes immediately
        startProfileListener();

        binding.btnEditProfile.setOnClickListener(v -> openEditDialog());
        binding.btnEditIcon.setOnClickListener(v -> openEditDialog());

        // Allow clicking profile image to upload new one
        binding.imgProfile.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    // --- FIX 1: USE SNAPSHOT LISTENER INSTEAD OF .GET() ---
    private void startProfileListener() {
        String uid = auth.getCurrentUser().getUid();

        // Refresh counts when profile loads
        ProfileCountManager.getInstance().refreshAllCounts();

        // This listens to the database live.
        // Whenever you save, this runs AUTOMATICALLY and updates the UI.
        profileListener = firestore.collection("Users").document(uid)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (document != null && document.exists()) {
                        currentProfile = document.toObject(ProfileModel.class);
                        updateUI(currentProfile);
                    } else {
                        // Default for new users
                        currentProfile = new ProfileModel("New User", "@new_user", "No headline yet", "Unknown",
                                "Welcome to my profile!");
                        updateUI(currentProfile);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop listening to save battery/data when user leaves this screen
        if (profileListener != null)
            profileListener.remove();
    }

    private void updateUI(ProfileModel p) {
        if (p == null)
            return;
        binding.tvName.setText(p.name);
        binding.tvHandle.setText(p.handle != null ? p.handle : "@user");
        binding.tvHeadline.setText(p.headline);
        binding.tvLocation.setText(p.location);
        binding.tvAbout.setText(p.about);

        // Stats
        binding.tvProjectsCount.setText(String.valueOf(p.projectCount));
        binding.tvConnectionsCount.setText(String.valueOf(p.connectionCount));
        binding.tvViewsCount.setText(String.valueOf(p.viewCount));

        setupSocialLink(binding.linkGithub, p.github);
        setupSocialLink(binding.linkLinkedin, p.linkedin);
        setupSocialLink(binding.linkTwitter, p.twitter);

        binding.skillsContainer.removeAllViews();
        if (p.skills != null) {
            for (String skill : p.skills) {
                addSkillChip(skill.trim());
            }
        }

        // Load profile image
        if (p.profileImageUrl != null && !p.profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(p.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(binding.imgProfile);
        } else {
            binding.imgProfile.setImageResource(R.drawable.ic_person);
        }
    }

    private void addSkillChip(String skillName) {
        Chip chip = new Chip(getContext());
        chip.setText(skillName);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setChipStrokeColorResource(com.google.android.material.R.color.design_default_color_primary);
        chip.setChipStrokeWidth(1f);
        // Ensure you have a color resource or use Color.BLACK
        chip.setTextColor(getResources().getColor(R.color.text));
        binding.skillsContainer.addView(chip);
    }

    private void setupSocialLink(TextView view, String url) {
        if (url == null || url.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Invalid URL", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog(getContext());
        View d = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(d);

        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // close logic for NEWPROJECT Dialog Box
        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        EditText name = d.findViewById(R.id.editName);
        EditText headline = d.findViewById(R.id.editHeadline);
        EditText loc = d.findViewById(R.id.editLocation);
        EditText about = d.findViewById(R.id.editAbout);
        EditText gh = d.findViewById(R.id.editGithub);
        EditText skillsInput = d.findViewById(R.id.editSkills);
        Button btnSave = d.findViewById(R.id.btnSaveProfile); // Bind Button here

        if (currentProfile != null) {
            name.setText(currentProfile.name);
            headline.setText(currentProfile.headline);
            loc.setText(currentProfile.location);
            about.setText(currentProfile.about);
            gh.setText(currentProfile.github);

            // --- FIX 2: REPLACE String.join FOR COMPATIBILITY ---
            if (currentProfile.skills != null && !currentProfile.skills.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String s : currentProfile.skills) {
                    sb.append(s).append(",");
                }
                // Remove trailing comma
                if (sb.length() > 0)
                    sb.setLength(sb.length() - 1);
                skillsInput.setText(sb.toString());
            }
        }

        btnSave.setOnClickListener(v -> {
            // Disable button to prevent double clicks
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            Map<String, Object> data = new HashMap<>();
            data.put("name", name.getText().toString());
            data.put("headline", headline.getText().toString());
            data.put("location", loc.getText().toString());
            data.put("about", about.getText().toString());
            data.put("github", gh.getText().toString());

            String[] sArray = skillsInput.getText().toString().split(",");
            List<String> sList = new ArrayList<>();
            for (String s : sArray)
                if (!s.trim().isEmpty())
                    sList.add(s.trim());
            data.put("skills", sList);

            firestore.collection("Users").document(auth.getCurrentUser().getUid())
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(u -> {
                        Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // DO NOT CALL loadProfileData() here.
                        // The snapshot listener at the top will handle it automatically.
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("ProfileError", e.getMessage()); // Check Logcat
                    });
        });

        dialog.show();
    }

    private void uploadProfileImage(Uri imageUri) {
        String uid = auth.getCurrentUser().getUid();
        StorageReference ref = storage.getReference().child("profile_images/" + uid + ".jpg");

        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Update Firestore
                        firestore.collection("Users").document(uid)
                                .update("profileImageUrl", downloadUrl)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(), "Profile image updated!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to update Firestore: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}