package com.example.selahbookingsystem.ui.customer;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerRescheduleActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_DURATION_MINS = "extra_duration_mins";

    private SupabaseRestService api;

    private String bookingId;
    private int durationMins;

    private LocalDate selectedDate;
    private LocalTime selectedTime;

    private TextView tvPicked;
    private MaterialButton btnPickDate, btnPickTime, btnConfirm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reschedule);

        api = ApiClient.get().create(SupabaseRestService.class);

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        durationMins = getIntent().getIntExtra(EXTRA_DURATION_MINS, 60);

        if (TextUtils.isEmpty(bookingId)) {
            toast("Missing booking id");
            finish();
            return;
        }

        tvPicked = findViewById(R.id.tvPicked);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnConfirm = findViewById(R.id.btnConfirmReschedule);

        btnPickDate.setOnClickListener(v -> pickDate());
        btnPickTime.setOnClickListener(v -> pickTime());

        btnConfirm.setOnClickListener(v -> {
            if (selectedDate == null || selectedTime == null) {
                toast("Pick a new date and time");
                return;
            }
            submitReschedule();
        });

        updatePickedLabel();
    }

    private void pickDate() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select new date")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            // selection is UTC millis at midnight
            selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.of("Europe/Dublin"))
                    .toLocalDate();
            updatePickedLabel();
        });

        picker.show(getSupportFragmentManager(), "datePicker");
    }

    private void pickTime() {
        int hour = selectedTime != null ? selectedTime.getHour() : 12;
        int minute = selectedTime != null ? selectedTime.getMinute() : 0;

        new TimePickerDialog(this, (view, h, m) -> {
            selectedTime = LocalTime.of(h, m);
            updatePickedLabel();
        }, hour, minute, true).show();
    }

    private void updatePickedLabel() {
        String d = selectedDate == null ? "—" : selectedDate.toString();
        String t = selectedTime == null ? "—" : selectedTime.toString();
        tvPicked.setText("New time: " + d + " • " + t);
    }

    private void submitReschedule() {
        btnConfirm.setEnabled(false);

        ZonedDateTime start = ZonedDateTime.of(selectedDate, selectedTime, ZoneId.of("Europe/Dublin"));
        ZonedDateTime end = start.plusMinutes(durationMins);

        String newStartIso = start.toInstant().toString();
        String newEndIso = end.toInstant().toString();

        // 1) Update booking time in Supabase
        SupabaseRestService.BookingUpdateBody body =
                new SupabaseRestService.BookingUpdateBody("confirmed", newStartIso, newEndIso);

        api.updateBooking("eq." + bookingId, body).enqueue(new Callback<List<BookingDto>>() {
            @Override
            public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                if (response.isSuccessful()) {

                    // 2) Trigger rescheduled email via Edge Function
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("booking_id", bookingId);

                    api.fnBookingRescheduledEmail(payload).enqueue(new Callback<Map<String, Object>>() {
                        @Override public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) { }
                        @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) { }
                    });

                    toast("Rescheduled");
                    finish();

                } else {
                    btnConfirm.setEnabled(true);
                    toast("Reschedule failed (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                btnConfirm.setEnabled(true);
                toast("Network error");
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
