package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.model.ServiceItem;

import java.util.ArrayList;
import java.util.List;

public class ServicesAdapter extends RecyclerView.Adapter<ServicesAdapter.VH> {

    public interface OnServiceClick {
        void onClick(ServiceItem service);
    }

    private final List<ServiceItem> items = new ArrayList<>();
    private final OnServiceClick onServiceClick;

    public ServicesAdapter(OnServiceClick onServiceClick) {
        this.onServiceClick = onServiceClick;
    }

    public void submit(List<ServiceItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ServiceItem s = items.get(position);
        h.title.setText(s.name == null ? "Service" : s.name);

        String subtitle = "";
        if (s.durationMins != null) subtitle += s.durationMins + " mins";
        if (s.price != null) subtitle += (subtitle.isEmpty() ? "" : " • ") + "€" + s.price;
        h.subtitle.setText(subtitle);

        h.itemView.setOnClickListener(v -> onServiceClick.onClick(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
