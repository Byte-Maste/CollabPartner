package com.example.realtime_data_firebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicHolder> {

    List<TopicModel> list;
    Context context;

    public interface OnTopicClickListener {
        void onTopicClick(TopicModel model);

        void onTopicLongClick(TopicModel model);

        void onShareTopic(TopicModel model);
    }

    private OnTopicClickListener listener;

    public TopicAdapter(List<TopicModel> list, Context context, OnTopicClickListener listener) {
        this.list = list;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_topic, parent, false);
        return new TopicHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicHolder holder, int position) {
        TopicModel model = list.get(position);
        holder.title.setText(model.title);
        holder.desc.setText(model.description);
        holder.progressBar.setProgress(model.progress);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onTopicClick(model);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null)
                listener.onTopicLongClick(model);
            return true;
        });

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null)
                listener.onShareTopic(model);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class TopicHolder extends RecyclerView.ViewHolder {
        TextView title, desc;
        ProgressBar progressBar;
        android.widget.ImageView btnShare;

        public TopicHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.topicTitle);
            desc = itemView.findViewById(R.id.topicDesc);
            progressBar = itemView.findViewById(R.id.topicProgress);
            btnShare = itemView.findViewById(R.id.btnShareTopic);
        }
    }
}
