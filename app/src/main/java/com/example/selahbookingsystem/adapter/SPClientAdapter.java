package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;

import java.util.ArrayList;
import java.util.List;

public class SPClientAdapter extends RecyclerView.Adapter<SPClientAdapter.VH> {

    private final List<ClientCard> items;

    public SPClientAdapter(List<ClientCard> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public void update(List<ClientCard> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sp_client_card, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ClientCard item = items.get(position);

        holder.tvClientName.setText(item.name);
        holder.tvClientEmail.setText(item.email);
        holder.tvClientPhone.setText(item.phone);

        String countText = item.appointmentCount == 1
                ? "1 confirmed appointment"
                : item.appointmentCount + " confirmed appointments";

        holder.tvAppointmentCount.setText(countText);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvClientName, tvClientEmail, tvClientPhone, tvAppointmentCount;

        VH(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientEmail = itemView.findViewById(R.id.tvClientEmail);
            tvClientPhone = itemView.findViewById(R.id.tvClientPhone);
            tvAppointmentCount = itemView.findViewById(R.id.tvAppointmentCount);
        }
    }

    public static class ClientCard {
        public String clientId;
        public String name;
        public String email;
        public String phone;
        public int appointmentCount;

        public ClientCard(String clientId, String name, String email, String phone, int appointmentCount) {
            this.clientId = clientId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.appointmentCount = appointmentCount;
        }
    }
}