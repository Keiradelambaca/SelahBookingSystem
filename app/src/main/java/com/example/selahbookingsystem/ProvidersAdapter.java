package com.example.selahbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProvidersAdapter extends RecyclerView.Adapter<ProvidersAdapter.VH> {

    interface OnProviderClick { void onProviderClicked(SupabaseRestService.ProviderDto p); }

    private final List<SupabaseRestService.ProviderDto> items = new ArrayList<>();
    private final OnProviderClick listener;

    ProvidersAdapter(OnProviderClick listener) { this.listener = listener; }

    void submit(List<SupabaseRestService.ProviderDto> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.providerName);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        SupabaseRestService.ProviderDto p = items.get(pos);
        h.name.setText(p.full_name != null ? p.full_name : "(Unnamed)");
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onProviderClicked(p); });
    }

    @Override public int getItemCount() { return items.size(); }
}
