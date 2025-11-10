package com.example.selahbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.VH> {

    public static class Item {
        public String title;
        public String subtitle;
        public Item(String title, String subtitle){ this.title = title; this.subtitle = subtitle; }
    }

    private final List<Item> items = new ArrayList<>();

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.apptTitle);
            subtitle = itemView.findViewById(R.id.apptSubtitle);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Item it = items.get(position);
        holder.title.setText(it.title);
        holder.subtitle.setText(it.subtitle);
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void submit(List<Item> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }
}

