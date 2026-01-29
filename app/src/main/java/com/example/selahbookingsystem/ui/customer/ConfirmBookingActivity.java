package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.util.NailDurationCalculator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ConfirmBookingActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";
    public static final String EXTRA_SELECTED_MAP = "extra_selected_map";
    public static final String EXTRA_EST_MINS = "extra_est_mins";
    public static final String EXTRA_SELECTED_SLOT = "extra_selected_slot";
    public static final String EXTRA_SERVICE_ID = "extra_service_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_booking);

        // Read extras ONCE
        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        String currentUrl = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        String inspoUrl = getIntent().getStringExtra(EXTRA_INSPO_URI);
        String rawMap = getIntent().getStringExtra(EXTRA_SELECTED_MAP);
        int estMins = getIntent().getIntExtra(EXTRA_EST_MINS, 60);
        String slot = getIntent().getStringExtra(EXTRA_SELECTED_SLOT);

        // Build selections map for JSONB
        LinkedHashMap<String, String> sel = BookingBubblesActivity.deserializeSelections(rawMap);

        // Summary UI
        StringBuilder sb = new StringBuilder();
        sb.append("Provider: ").append(providerName == null ? "" : providerName).append("\n");
        sb.append("Time: ").append(slot == null ? "" : slot).append("\n");
        sb.append("Duration: ").append(NailDurationCalculator.pretty(estMins)).append("\n\n");
        sb.append("Details:\n");
        for (String k : sel.keySet()) {
            sb.append("• ").append(k).append(": ").append(sel.get(k)).append("\n");
        }

        TextView tvSummary = findViewById(R.id.tvSummary);
        tvSummary.setText(sb.toString());

        Button btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);

            // Basic checks
            if (providerId == null || providerId.trim().isEmpty()) {
                Toast.makeText(this, "Missing provider id", Toast.LENGTH_LONG).show();
                btnConfirm.setEnabled(true);
                return;
            }

            if (slot == null || slot.trim().isEmpty()) {
                Toast.makeText(this, "Missing time slot", Toast.LENGTH_LONG).show();
                btnConfirm.setEnabled(true);
                return;
            }

            String clientId = TokenStore.getUserId(this);
            if (clientId == null || clientId.trim().isEmpty()) {
                Toast.makeText(this, "You must be logged in (client_id missing)", Toast.LENGTH_LONG).show();
                btnConfirm.setEnabled(true);
                return;
            }

            // Convert slot (e.g. "Tomorrow • 11:30") to ISO timestamptz strings
            String startTimeIso;
            String endTimeIso;

            try {
                String[] parts = slot.split("•");
                if (parts.length != 2) throw new IllegalArgumentException("Bad slot: " + slot);

                String left = parts[0].trim();      // "Tomorrow" OR "Fri 23 Feb"
                String timePart = parts[1].trim();  // "11:30"

                LocalDate date;
                if (left.equalsIgnoreCase("tomorrow")) {
                    date = LocalDate.now().plusDays(1);
                } else if (left.equalsIgnoreCase("today")) {
                    date = LocalDate.now();
                } else {
                    // "Fri 23 Feb" -> attach current year
                    date = LocalDate.parse(
                            left + " " + LocalDate.now().getYear(),
                            DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ENGLISH)
                    );
                }

                LocalTime time = LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));

                ZonedDateTime zonedStart = ZonedDateTime.of(date, time, ZoneId.systemDefault());
                ZonedDateTime zonedEnd = zonedStart.plusMinutes(estMins);

                startTimeIso = zonedStart.toInstant().toString();
                endTimeIso = zonedEnd.toInstant().toString();

            } catch (Exception e) {
                Log.e("BOOKING", "Slot parse failed: " + slot, e);
                Toast.makeText(this, "Invalid slot format: " + slot, Toast.LENGTH_LONG).show();
                btnConfirm.setEnabled(true);
                return;
            }

            // Build booking insert body (keys MUST match DB column names)
            Map<String, Object> body = new HashMap<>();
            body.put("client_id", clientId);
            body.put("provider_id", providerId);

            body.put("provider_name", providerName);
            body.put("start_time", startTimeIso);
            body.put("end_time", endTimeIso);

            body.put("duration_mins", estMins);
            body.put("details_json", sel);

            body.put("current_photo_url", currentUrl);
            body.put("inspo_photo_url", inspoUrl);

            body.put("status", "CONFIRMED");

            ApiClient.bookings().createBooking(body, "*").enqueue(
                    new retrofit2.Callback<java.util.List<java.util.Map<String, Object>>>() {
                        @Override
                        public void onResponse(
                                retrofit2.Call<java.util.List<java.util.Map<String, Object>>> call,
                                retrofit2.Response<java.util.List<java.util.Map<String, Object>>> response
                        ) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                Toast.makeText(ConfirmBookingActivity.this, "✅ Booking confirmed!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                String err = "";
                                try { if (response.errorBody() != null) err = response.errorBody().string(); } catch (Exception ignored) {}
                                Log.e("BOOKING", "Insert failed code=" + response.code() + " err=" + err);
                                Toast.makeText(ConfirmBookingActivity.this, "❌ Booking failed (" + response.code() + ")", Toast.LENGTH_LONG).show();
                                btnConfirm.setEnabled(true);
                            }
                        }

                        @Override
                        public void onFailure(
                                retrofit2.Call<java.util.List<java.util.Map<String, Object>>> call,
                                Throwable t
                        ) {
                            Log.e("BOOKING", "Network error", t);
                            Toast.makeText(ConfirmBookingActivity.this, "🔥 Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            btnConfirm.setEnabled(true);
                        }
                    }
            );
        });
    }
}
