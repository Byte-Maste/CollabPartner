package com.example.realtime_data_firebase;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectHolder> {

    List<ProjectModel> list;
    OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete(ProjectModel project);
    }

    public ProjectAdapter(List<ProjectModel> list, OnDeleteListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectHolder h, int i) {
        ProjectModel p = list.get(i);

        h.title.setText(p.title);
        h.desc.setText(p.description);
        h.github.setText(p.github);

        // --- CORE LOGIC: LIVE DEMO REDIRECT ---
        // 1. Check if link exists
        if (p.github == null || p.github.trim().isEmpty()) {
            h.btnLiveDemo.setVisibility(View.GONE); // Hide button if no link
        } else {
            h.btnLiveDemo.setVisibility(View.VISIBLE);

            h.btnLiveDemo.setOnClickListener(v -> {
                String url = p.github.trim();
                // 2. Ensure URL has protocol
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                // 3. Open Browser
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    h.itemView.getContext().startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(h.itemView.getContext(), "Invalid Link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- INNOVATION: ONE-CLICK SHARE ---
        // Allow users to share the project details to WhatsApp/etc by clicking the title
        h.title.setOnClickListener(v -> {
            String shareBody = "Check out this project: " + p.title + "\n\n" + p.description + "\n\nLink: " + p.github;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Project Share");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            h.itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
        // -----------------------------------

        // Long press to show delete
        h.itemView.setOnLongClickListener(v -> {
            h.btnDelete.setVisibility(View.VISIBLE);
            return true;
        });

        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ProjectHolder extends RecyclerView.ViewHolder {
        TextView title, desc, github;
        ImageView btnDelete;
        Button btnLiveDemo; // Defined the button here

        public ProjectHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.projectTitle);
            desc = itemView.findViewById(R.id.projectDesc);
            github = itemView.findViewById(R.id.githubLink);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Map the button (It was named btnAttendEvent in your XML)
            btnLiveDemo = itemView.findViewById(R.id.btnLiveDemo);
        }
    }
}