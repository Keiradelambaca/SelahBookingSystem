package com.example.selahbookingsystem.adapter;

import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class WeeklyAvailabilityAdapter extends RecyclerView.Adapter<WeeklyAvailabilityAdapter.VH> {

    public interface OnChanged { void onChanged(); }

    private final Context context;
    private final List<WeeklyAvailabilityDto> items;
    private final OnChanged onChanged;

    public WeeklyAvailabilityAdapter(Context context, List<WeeklyAvailabilityDto> items, OnChanged onChanged) {
        this.context = context;
        this.items = items;
        this.onChanged = onChanged;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weekly_availability, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WeeklyAvailabilityDto row = items.get(position);

        h.tvDay.setText(dayLabel(row.day_of_week));

        h.switchEnabled.setOnCheckedChangeListener(null);
        h.switchEnabled.setChecked(row.enabled);
        h.switchEnabled.setOnCheckedChangeListener((btn, isChecked) -> {
            row.enabled = isChecked;
            onChanged.onChanged();
        });

        h.btnStart.setText(formatTime(row.start_time));
        h.btnEnd.setText(formatTime(row.end_time));

        h.btnStart.setOnClickListener(v -> showTimePicker(row, true, h));
        h.btnEnd.setOnClickListener(v -> showTimePicker(row, false, h));
    }

    private void showTimePicker(WeeklyAvailabilityDto row, boolean isStart, VH h) {
        String current = isStart ? row.start_time : row.end_time;
        int[] hm = parseTime(current);

        TimePickerDialog dlg = new TimePickerDialog(context, (TimePicker view, int hourOfDay, int minute) -> {

            // snap to 30 mins
            int snappedMin = (minute < 15) ? 0 : (minute < 45 ? 30 : 0);
            int snappedHour = hourOfDay + (minute >= 45 ? 1 : 0);
            if (snappedHour >= 24) snappedHour = 23;

            String val = String.format(Locale.US, "%02d:%02d:00", snappedHour, snappedMin);

            if (isStart) row.start_time = val;
            else row.end_time = val;

            int startM = toMinutes(row.start_time);
            int endM = toMinutes(row.end_time);

            if (endM <= startM) {
                endM = startM + 30;
                if (endM > 23 * 60 + 59) endM = 23 * 60 + 59;
                row.end_time = String.format(Locale.US, "%02d:%02d:00", endM / 60, endM % 60);
            }

            h.btnStart.setText(formatTime(row.start_time));
            h.btnEnd.setText(formatTime(row.end_time));
            onChanged.onChanged();

        }, hm[0], hm[1], true);

        dlg.show();
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDay;
        SwitchMaterial switchEnabled;
        MaterialButton btnStart, btnEnd;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
            btnStart = itemView.findViewById(R.id.btnStart);
            btnEnd = itemView.findViewById(R.id.btnEnd);
        }
    }

    private String dayLabel(int dow) {
        switch (dow) {
            case 0: return "Sunday";
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            case 4: return "Thursday";
            case 5: return "Friday";
            case 6: return "Saturday";
            default: return "Day";
        }
    }

    private int[] parseTime(String hhmmss) {
        try {
            String[] parts = hhmmss.split(":");
            return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
        } catch (Exception e) {
            return new int[]{9, 0};
        }
    }

    private String formatTime(String hhmmss) {
        int[] hm = parseTime(hhmmss);
        return String.format(Locale.US, "%02d:%02d", hm[0], hm[1]);
    }

    private int toMinutes(String hhmmss) {
        int[] hm = parseTime(hhmmss);
        return hm[0] * 60 + hm[1];
    }
}
