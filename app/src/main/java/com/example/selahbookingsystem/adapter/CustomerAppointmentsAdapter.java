package com.example.selahbookingsystem.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.CustomerAppointment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerAppointmentsAdapter
        extends RecyclerView.Adapter<CustomerAppointmentsAdapter.AppointmentViewHolder> {

    public interface OnAppointmentClickListener {
        void onAppointmentClicked(CustomerAppointment appointment);
    }

    private final Context context;
    private final List<CustomerAppointment> appointments;
    private final OnAppointmentClickListener listener;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("EEE dd MMM • HH:mm")
                    .withZone(ZoneId.systemDefault());

    public CustomerAppointmentsAdapter(
            Context context,
            List<CustomerAppointment> appointments,
            OnAppointmentClickListener listener
    ) {
        this.context = context;
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AppointmentViewHolder holder,
            int position
    ) {
        CustomerAppointment appointment = appointments.get(position);

        // ---- Text Binding ----
        holder.tvServiceTitle.setText(appointment.getServiceTitle());
        holder.tvProviderName.setText(appointment.getProviderName());
        holder.tvLocation.setText(appointment.getLocationArea());
        holder.tvPrice.setText("€" + String.format("%.2f", appointment.getPrice()));

        if (appointment.getAppointmentStart() != null) {
            holder.tvDateTime.setText(dateFormatter.format(appointment.getAppointmentStart()));
        } else {
            holder.tvDateTime.setText("Time TBC");
        }

        // ---- Payment Status ----
        switch (appointment.getPaymentStatus()) {
            case DEPOSIT_PAID:
                holder.chipPaymentStatus.setText("Deposit paid");
                break;
            case DEPOSIT_NOT_PAID:
                holder.chipPaymentStatus.setText("Deposit not paid");
                break;
            case PAID:
                holder.chipPaymentStatus.setText("Paid");
                break;
            case PAY_BY_CASH:
                holder.chipPaymentStatus.setText("Pay by cash");
                break;
        }

        // ---- Banner Image ----
        if (appointment.getBannerUrl() != null &&
                !appointment.getBannerUrl().isEmpty()) {

            Glide.with(context)
                    .load(appointment.getBannerUrl())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_banner) // optional
                    .into(holder.ivBanner);

        } else {
            holder.ivBanner.setImageResource(R.drawable.placeholder_banner);
        }

        // ---- Card Click ----
        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClicked(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // -----------------------------
    // ViewHolder
    // -----------------------------
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardRoot;
        ImageView ivBanner;
        TextView tvServiceTitle, tvProviderName, tvDateTime, tvLocation, tvPrice;
        Chip chipPaymentStatus;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);

            cardRoot = itemView.findViewById(R.id.cardRoot);
            ivBanner = itemView.findViewById(R.id.ivBanner);
            tvServiceTitle = itemView.findViewById(R.id.tvServiceTitle);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            chipPaymentStatus = itemView.findViewById(R.id.chipPaymentStatus);
        }
    }
}
