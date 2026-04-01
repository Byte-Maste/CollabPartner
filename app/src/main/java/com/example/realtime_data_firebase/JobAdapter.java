package com.example.realtime_data_firebase;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobHolder> {

    List<JobModel> list;
    String currentUserId;
    OnJobActionListener listener;

    public interface OnJobActionListener {
        void onDelete(JobModel job);
    }

    public JobAdapter(List<JobModel> list, String currentUserId, OnJobActionListener listener) {
        this.list = list;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
        return new JobHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull JobHolder h, int i) {
        JobModel job = list.get(i);

        h.title.setText(job.title);
        h.company.setText(job.company);
        h.desc.setText(job.description);
        h.location.setText(job.location);
        h.budget.setText(job.budget);
        h.deadline.setText("Due: " + job.deadline);
        h.applicants.setText(job.applicantCount + " applicants");

        // Format Skills: Replace comma with bullets for cleaner look
        if (job.skills != null) {
            h.skills.setText(job.skills.replace(",", "  •  "));
        }

        // Set Type Tag Color
        h.typeTag.setText(job.type);
        if ("Freelance".equalsIgnoreCase(job.type)) {
            h.typeTag.setTextColor(Color.parseColor("#4CAF50")); // Green
            h.typeTag.setBackgroundResource(R.drawable.tag_bg_green); // You need a green drawable
        } else {
            h.typeTag.setTextColor(Color.parseColor("#2196F3")); // Blue
            h.typeTag.setBackgroundResource(R.drawable.tag_bg_blue);
        }

        // --- OWNER LOGIC: SHOW DELETE ---
        if (job.creatorId != null && job.creatorId.equals(currentUserId)) {
            h.btnDelete.setVisibility(View.VISIBLE);
            h.btnDelete.setOnClickListener(v -> listener.onDelete(job));
        } else {
            h.btnDelete.setVisibility(View.GONE);
        }

        // --- INNOVATION: ONE-TAP APPLY ---
        h.btnApply.setOnClickListener(v -> {
            // Increment local count immediately for UI speed
            int newCount = job.applicantCount + 1;
            h.applicants.setText(newCount + " applicants");
            h.btnApply.setText("Applied");
            h.btnApply.setEnabled(false); // Prevent double apply

            // Update Firestore
            FirebaseFirestore.getInstance().collection("Jobs")
                    .document(job.jobId)
                    .update("applicantCount", newCount)
                    .addOnSuccessListener(a -> Toast.makeText(h.itemView.getContext(), "Applied successfully!", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class JobHolder extends RecyclerView.ViewHolder {
        TextView title, company, desc, location, budget, deadline, skills, typeTag, applicants;
        Button btnApply;
        ImageView btnDelete;

        public JobHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.jobTitle);
            company = itemView.findViewById(R.id.jobCompany);
            desc = itemView.findViewById(R.id.jobDesc);
            location = itemView.findViewById(R.id.jobLocation);
            budget = itemView.findViewById(R.id.jobBudget);
            deadline = itemView.findViewById(R.id.jobDeadline);
            skills = itemView.findViewById(R.id.jobSkills);
            typeTag = itemView.findViewById(R.id.jobTypeTag);
            applicants = itemView.findViewById(R.id.txtApplicants);
            btnApply = itemView.findViewById(R.id.btnApply);
            btnDelete = itemView.findViewById(R.id.btnDeleteJob);
        }
    }
}