package com.example.realtime_data_firebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    List<NoteModel> list;

    public NoteAdapter(List<NoteModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHolder holder, int position) {
        NoteModel model = list.get(position);
        holder.sender.setText(model.senderName);
        holder.text.setText(model.text);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class NoteHolder extends RecyclerView.ViewHolder {
        TextView sender, text;

        public NoteHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.noteSender);
            text = itemView.findViewById(R.id.noteText);
        }
    }
}
