package com.example.selahbookingsystem.adapter;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.model.ClientSummary;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.VH> {

    public interface OnChargeClick {
        void onCharge(ClientSummary client);
    }

    private final List<ClientSummary> items = new ArrayList<>();
    private final OnChargeClick onChargeClick;

    public ClientsAdapter(OnChargeClick onChargeClick) {
        this.onChargeClick = onChargeClick;
    }

    public void submitList(List<ClientSummary> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ClientSummary c = items.get(position);

        h.tvName.setText(c.fullName != null ? c.fullName : "Unknown client");
        h.tvEmail.setText(c.email != null ? c.email : "—");
        h.tvPhone.setText(c.phone != null ? c.phone : "—");

        h.tvTimesBooked.setText("Times booked: " + c.timesBooked);
        h.tvLastAppointment.setText(c.lastAppointmentText != null ? c.lastAppointmentText : "Last appointment: —");
        h.tvPayment.setText(c.paymentText != null ? c.paymentText : "Saved card: —");

        h.expandedSection.setVisibility(c.expanded ? View.VISIBLE : View.GONE);

        h.card.setOnClickListener(v -> {
            // Animate expand/collapse
            TransitionManager.beginDelayedTransition((ViewGroup) h.card, new AutoTransition());
            c.expanded = !c.expanded;
            notifyItemChanged(h.getAdapterPosition());
        });

        h.btnCharge.setOnClickListener(v -> {
            if (onChargeClick != null) onChargeClick.onCharge(c);
            else Toast.makeText(v.getContext(), "Charge clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName, tvEmail, tvPhone;
        View expandedSection;
        TextView tvTimesBooked, tvLastAppointment, tvPayment;
        MaterialButton btnCharge;

        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardClient);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);

            expandedSection = itemView.findViewById(R.id.expandedSection);
            tvTimesBooked = itemView.findViewById(R.id.tvTimesBooked);
            tvLastAppointment = itemView.findViewById(R.id.tvLastAppointment);
            tvPayment = itemView.findViewById(R.id.tvPayment);
            btnCharge = itemView.findViewById(R.id.btnChargeRemainder);
        }
    }
}