package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.model.ProviderServiceConfigItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SPServicePricingAdapter extends RecyclerView.Adapter<SPServicePricingAdapter.VH> {

    public interface OnEditClickListener {
        void onEditClicked(ProviderServiceConfigItem item);
    }

    private final List<ProviderServiceConfigItem> items = new ArrayList<>();
    private final OnEditClickListener listener;

    public SPServicePricingAdapter(OnEditClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProviderServiceConfigItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sp_service_pricing, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ProviderServiceConfigItem item = items.get(position);

        String serviceName = item.service_name != null ? item.service_name : "";
        String category = prettyCategory(item.service_category);
        boolean isOffered = item.is_offered;

        int priceCents = item.price_cents;
        int durationMins = item.duration_mins;

        h.tvName.setText(serviceName);
        h.tvCategory.setText(category);
        h.switchOffered.setChecked(isOffered);
        h.switchOffered.setText("");
        h.switchOffered.setShowText(false);

        String price = String.format(Locale.getDefault(), "€%.2f", priceCents / 100.0);
        String mins = durationMins + " mins";
        h.tvMeta.setText(price + " • " + mins);

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvMeta;
        MaterialSwitch switchOffered;
        MaterialButton btnEdit;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvServiceName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            switchOffered = itemView.findViewById(R.id.switchOffered);
            btnEdit = itemView.findViewById(R.id.btnEdit);

            switchOffered.setText("");
        }
    }

    private String prettyCategory(String raw) {
        if (raw == null) return "";
        switch (raw) {
            case "base":
                return "Base service";
            case "addon":
                return "Add-on";
            case "complexity":
                return "Design complexity";
            default:
                return raw;
        }
    }
}