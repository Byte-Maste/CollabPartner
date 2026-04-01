package com.example.realtime_data_firebase;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class JobFragment extends Fragment {

    FirebaseFirestore firestore;
    FirebaseAuth auth;
    List<JobModel> jobList = new ArrayList<>();
    JobAdapter adapter;
    RecyclerView recyclerView;

    public JobFragment() { super(R.layout.fragment_job); }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.jobRecycler);
        Button btnPost = view.findViewById(R.id.btnPostJob);

        String uid = auth.getCurrentUser().getUid();
        adapter = new JobAdapter(jobList, uid, this::deleteJob);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        Spinner spinnerFilter = view.findViewById(R.id.spinnerFilter);

        // 2. Create an Adapter using the string array we just made
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.job_types, // The array from strings.xml
                android.R.layout.simple_spinner_item // Default layout for the selected item
        );

        // 3. Specify the layout to use when the list of choices appears
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 4. Attach the adapter to the spinner
        spinnerFilter.setAdapter(spinnerAdapter);

        loadJobs();

        btnPost.setOnClickListener(v -> openCreateDialog());
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog(getContext());
        View d = LayoutInflater.from(getContext()).inflate(R.layout.dialog_job, null);
        dialog.setContentView(d);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: Removes default white borders
        }

        //close logic for NEWPROJECT Dialog Box
        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Bind Views
        EditText title = d.findViewById(R.id.inputJobTitle);
        EditText desc = d.findViewById(R.id.inputJobDesc);
        EditText company = d.findViewById(R.id.inputCompany);
        EditText location = d.findViewById(R.id.inputJobLocation);
        EditText budget = d.findViewById(R.id.inputBudget);
        EditText deadline = d.findViewById(R.id.inputDeadline);
        EditText skills = d.findViewById(R.id.inputSkills);
        Spinner typeSpinner = d.findViewById(R.id.spinnerType);
        Button submit = d.findViewById(R.id.btnSubmitJob);

        submit.setOnClickListener(v -> {
            if(title.getText().toString().isEmpty()) { title.setError("Required"); return; }

            saveJob(
                    title.getText().toString(),
                    desc.getText().toString(),
                    company.getText().toString(),
                    location.getText().toString(),
                    typeSpinner.getSelectedItem().toString(),
                    budget.getText().toString(),
                    deadline.getText().toString(),
                    skills.getText().toString(),
                    dialog
            );
        });

        dialog.show();
    }

    private void saveJob(String t, String ds, String comp, String loc, String typ, String bud, String dead, String sk, Dialog dialog) {
        String uid = auth.getCurrentUser().getUid();
        String jobId = UUID.randomUUID().toString();

        JobModel job = new JobModel(jobId, uid, t, ds, comp, loc, typ, bud, dead, sk);

        firestore.collection("Jobs").document(jobId).set(job)
                .addOnSuccessListener(u -> {
                    Toast.makeText(getContext(), "Job Posted!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show());
    }

    private void loadJobs() {
        firestore.collection("Jobs")
                .orderBy("postedDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    jobList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        jobList.add(doc.toObject(JobModel.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void deleteJob(JobModel job) {
        firestore.collection("Jobs").document(job.jobId).delete()
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Job Removed", Toast.LENGTH_SHORT).show());
    }
}