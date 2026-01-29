package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.ui.customer.ConfirmBookingActivity;

import java.util.ArrayList;
import java.util.List;

public class TimeslotsAdapter extends RecyclerView.Adapter<TimeslotsAdapter.VH> {

    public interface OnSlotClick {
        void onClick(String slotLabel);
    }

    private final List<String> items = new ArrayList<>();
    private final OnSlotClick onSlotClick;

    public TimeslotsAdapter(OnSlotClick onSlotClick) {
        this.onSlotClick = onSlotClick;
    }

    public void submit(List<String> slots) {
        items.clear();
        if (slots != null) items.addAll(slots);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeslot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String label = items.get(position);
        h.tvWhen.setText(label);
        h.itemView.setOnClickListener(v -> onSlotClick.onClick(label));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvWhen;
        VH(@NonNull View itemView) {
            super(itemView);
            tvWhen = itemView.findViewById(R.id.tvWhen);
        }
    }
}

