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
    private final boolean compactMode; // true = homepage card style

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("EEE dd MMM • HH:mm")
                    .withZone(ZoneId.systemDefault());

    public CustomerAppointmentsAdapter(
            Context context,
            List<CustomerAppointment> appointments,
            OnAppointmentClickListener listener
    ) {
        this(context, appointments, listener, false);
    }

    public CustomerAppointmentsAdapter(
            Context context,
            List<CustomerAppointment> appointments,
            OnAppointmentClickListener listener,
            boolean compactMode
    ) {
        this.context = context;
        this.appointments = appointments;
        this.listener = listener;
        this.compactMode = compactMode;
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

        // Title
        String serviceTitle = appointment.getServiceTitle();
        if (serviceTitle == null || serviceTitle.trim().isEmpty()) {
            serviceTitle = "Nail Appointment";
        }
        holder.tvServiceTitle.setText(serviceTitle);

        // Provider
        String providerName = appointment.getProviderName();
        if (providerName == null || providerName.trim().isEmpty()) {
            providerName = "Provider";
        }
        holder.tvProviderName.setText(providerName);

        // Date/time
        if (appointment.getAppointmentStart() != null) {
            holder.tvDateTime.setText(dateFormatter.format(appointment.getAppointmentStart()));
        } else {
            holder.tvDateTime.setText("Time TBC");
        }

        // Location
        String location = appointment.getLocationArea();
        if (location == null || location.trim().isEmpty()) {
            location = "Near you";
        }
        holder.tvLocation.setText(location);

        // Price
        holder.tvPrice.setText(String.format("€%.2f", appointment.getPrice()));

        // Payment status
        CustomerAppointment.PaymentStatus paymentStatus = appointment.getPaymentStatus();
        if (paymentStatus == null) {
            paymentStatus = CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID;
        }

        switch (paymentStatus) {
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

        // Banner image
        if (appointment.getBannerUrl() != null && !appointment.getBannerUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(appointment.getBannerUrl())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_banner)
                    .error(R.drawable.placeholder_banner)
                    .into(holder.ivBanner);
        } else {
            holder.ivBanner.setImageResource(R.drawable.placeholder_banner);
        }

        // Compact homepage mode:
        // show image + title + date/time only
        if (compactMode) {
            holder.tvProviderName.setVisibility(View.GONE);
            holder.tvLocation.setVisibility(View.GONE);
            holder.priceRow.setVisibility(View.GONE);
        } else {
            holder.tvProviderName.setVisibility(View.VISIBLE);
            holder.tvLocation.setVisibility(View.VISIBLE);
            holder.priceRow.setVisibility(View.VISIBLE);
        }

        // Click
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

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardRoot;
        ImageView ivBanner;
        TextView tvServiceTitle, tvProviderName, tvDateTime, tvLocation, tvPrice;
        Chip chipPaymentStatus;
        View priceRow;

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
            priceRow = itemView.findViewById(R.id.priceRow);
        }
    }
}
