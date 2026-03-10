package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.ServiceDto;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SPServicesAdapter extends RecyclerView.Adapter<SPServicesAdapter.VH> {

    public interface OnServiceClick {
        void onClick(@NonNull ServiceDto service);
    }

    private final List<ServiceDto> items = new ArrayList<>();
    private final OnServiceClick onClick;

    public SPServicesAdapter(@NonNull OnServiceClick onClick) {
        this.onClick = onClick;
    }

    public void submitList(List<ServiceDto> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        ServiceDto s = items.get(position);
        // If you changed id to Integer in ServiceDto, this is perfect.
        if (s != null && s.id != null) return s.id;
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_sp_service, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ServiceDto s = items.get(position);

        String name = (s.name == null) ? "" : s.name.trim();
        h.tvName.setText(name.isEmpty() ? "Service" : name);

        int priceCents = (s.base_price_cents == null) ? 0 : s.base_price_cents;
        int mins = (s.base_duration_mins == null) ? 0 : s.base_duration_mins;

        h.chipPrice.setText(String.format(Locale.getDefault(), "€%.2f", priceCents / 100.0));
        h.chipTime.setText(String.format(Locale.getDefault(), "%d mins", mins));

        h.card.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvName;
        final Chip chipPrice;
        final Chip chipTime;

        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardService);
            tvName = itemView.findViewById(R.id.tvServiceName);
            chipPrice = itemView.findViewById(R.id.chipPrice);
            chipTime = itemView.findViewById(R.id.chipTime);
        }
    }
}