package com.example.realtime_data_firebase;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.realtime_data_firebase.databinding.FragmentProjectBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ProjectFragment extends Fragment {

    FragmentProjectBinding binding;
    FirebaseFirestore firestore;
    FirebaseAuth auth;
    List<ProjectModel> projectList = new ArrayList<>();
    ProjectAdapter adapter;

    public ProjectFragment() {
        super(R.layout.fragment_project);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding = FragmentProjectBinding.bind(view);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ProjectAdapter(projectList, project -> deleteProject(project));

        binding.projectRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.projectRecycler.setAdapter(adapter);

        loadProjects();

        binding.btnNewProject.setOnClickListener(v -> openCreateDialog());
    }

    private void deleteProject(ProjectModel project) {
        String uid = auth.getCurrentUser().getUid();

        firestore.collection("Users")
                .document(uid)
                .collection("Projects")
                .document(project.projectId)
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Project deleted", Toast.LENGTH_SHORT).show();

                    // Update project count in profile
                    ProfileCountManager.getInstance().updateProjectCount();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog(getContext());
        View d = LayoutInflater.from(getContext()).inflate(R.layout.project_dialog, null);
        dialog.setContentView(d);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: Removes default
                                                                                           // white borders
        }
        // close logic for NEWPROJECT Dialog Box
        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        EditText title = d.findViewById(R.id.inputProjectTitle);
        EditText desc = d.findViewById(R.id.inputProjectDesc);
        EditText github = d.findViewById(R.id.inputGithub);
        Button create = d.findViewById(R.id.btnCreateProject);

        create.setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            String ds = desc.getText().toString().trim();
            String gh = github.getText().toString().trim();

            if (t.isEmpty()) {
                title.setError("Required");
                return;
            }
            if (ds.isEmpty()) {
                desc.setError("Required");
                return;
            }
            if (gh.isEmpty()) {
                github.setError("Reguired");
                return;
            }
            saveProject(t, ds, gh, dialog);
        });

        dialog.show();
    }

    private void saveProject(String title, String desc, String github, Dialog dialog) {

        String uid = auth.getCurrentUser().getUid();
        String projectId = UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", desc);
        data.put("github", github);
        data.put("projectId", projectId);
        data.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("Users")
                .document(uid)
                .collection("Projects")
                .document(projectId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Project created!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadProjects(); // auto refresh

                    // Update project count in profile
                    ProfileCountManager.getInstance().updateProjectCount();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadProjects() {
        assert auth.getCurrentUser() != null;
        String uid = auth.getCurrentUser().getUid();

        firestore.collection("Users")
                .document(uid)
                .collection("Projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null)
                        return;

                    projectList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ProjectModel m = doc.toObject(ProjectModel.class);
                        projectList.add(m);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
