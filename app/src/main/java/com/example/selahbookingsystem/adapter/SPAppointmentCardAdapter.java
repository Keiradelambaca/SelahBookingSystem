package com.example.selahbookingsystem.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.ui.provider.SPAppointmentDetailsActivity;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SPAppointmentCardAdapter extends RecyclerView.Adapter<SPAppointmentCardAdapter.VH> {

    private final Context context;
    private final List<BookingDto> items;

    public SPAppointmentCardAdapter(Context context, List<BookingDto> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sp_appointment_card, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BookingDto item = items.get(position);

        String clientName = safe(item.client_name, "Client");
        String serviceName = buildServiceText(item);

        holder.tvClientName.setText(clientName);
        holder.tvServiceName.setText(serviceName);
        holder.tvDateTime.setText(formatDateTimeRange(item.start_time, item.end_time));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SPAppointmentDetailsActivity.class);
            intent.putExtra("booking_id", item.id);
            intent.putExtra("client_name", clientName);
            intent.putExtra("service_name", serviceName);
            intent.putExtra("start_time", item.start_time);
            intent.putExtra("end_time", item.end_time);
            intent.putExtra("inspo_photo_url", item.inspo_photo_url);
            intent.putExtra("status", item.status);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvClientName, tvServiceName, tvDateTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }

    private String buildServiceText(BookingDto item) {
        if (item.duration_mins != null && item.duration_mins > 0) {
            return item.duration_mins + " min appointment";
        }
        return safe(item.status, "Appointment");
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String formatDateTimeRange(String startIso, String endIso) {
        try {
            Date start = parseIso(startIso);
            Date end = parseIso(endIso);

            if (start == null || end == null) return "Date unavailable";

            SimpleDateFormat dateFmt = new SimpleDateFormat("EEE d MMM", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());

            return dateFmt.format(start) + " • " + timeFmt.format(start) + " - " + timeFmt.format(end);
        } catch (Exception e) {
            return "Date unavailable";
        }
    }

    private Date parseIso(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        try {
            return Date.from(
                    OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            .toInstant()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
