package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.util.NailDurationCalculator;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmBookingActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";
    public static final String EXTRA_SELECTED_MAP = "extra_selected_map";
    public static final String EXTRA_EST_MINS = "extra_est_mins";
    public static final String EXTRA_SELECTED_SLOT = "extra_selected_slot";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_booking);

        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        String currentUrl = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        String inspoUrl = getIntent().getStringExtra(EXTRA_INSPO_URI);
        String rawMap = getIntent().getStringExtra(EXTRA_SELECTED_MAP);
        int estMins = getIntent().getIntExtra(EXTRA_EST_MINS, 60);
        String slotLabel = getIntent().getStringExtra(EXTRA_SELECTED_SLOT);

        LinkedHashMap<String, String> selections =
                BookingBubblesActivity.deserializeSelections(rawMap);

        TextView tvSummary = findViewById(R.id.tvSummary);
        tvSummary.setText(buildSummary(providerName, slotLabel, estMins, selections));

        Button btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);

            if (TextUtils.isEmpty(providerId) || TextUtils.isEmpty(slotLabel)) {
                toast("Missing booking info");
                btnConfirm.setEnabled(true);
                return;
            }

            String clientId = TokenStore.getUserId(this);
            if (TextUtils.isEmpty(clientId)) {
                toast("You must be logged in");
                btnConfirm.setEnabled(true);
                return;
            }

            ZonedDateTime startZdt;
            try {
                startZdt = parseSlot(slotLabel);
            } catch (Exception e) {
                toast("Invalid time slot");
                btnConfirm.setEnabled(true);
                return;
            }

            ZonedDateTime endZdt = startZdt.plusMinutes(estMins);

            BookingDto body = new BookingDto();
            body.client_id = clientId;
            body.provider_id = providerId;
            body.provider_name = providerName;
            body.start_time = startZdt.toInstant().toString();
            body.end_time = endZdt.toInstant().toString();
            body.duration_mins = estMins;
            body.details_json = selections;
            body.current_photo_url = currentUrl;
            body.inspo_photo_url = inspoUrl;
            body.status = "confirmed";

            SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);
            api.createBooking(body).enqueue(new Callback<List<BookingDto>>() {
                @Override
                public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        String bookingId = response.body().get(0).id;

                        Intent i = new Intent(ConfirmBookingActivity.this, BookingConfirmedActivity.class);
                        i.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID, bookingId);
                        startActivity(i);
                        finish();
                    } else {
                        toast("Booking failed");
                        btnConfirm.setEnabled(true);
                    }
                }

                @Override
                public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                    toast("Network error");
                    btnConfirm.setEnabled(true);
                }
            });
        });
    }

    private String buildSummary(String provider, String slot, int mins,
                                LinkedHashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("Provider: ").append(provider).append("\n");
        sb.append("Time: ").append(slot).append("\n");
        sb.append("Duration: ").append(NailDurationCalculator.pretty(mins)).append("\n\n");
        for (String k : map.keySet()) {
            sb.append("• ").append(k).append(": ").append(map.get(k)).append("\n");
        }
        return sb.toString();
    }

    private ZonedDateTime parseSlot(String slot) {
        String[] parts = slot.split("•");
        String left = parts[0].trim();
        String time = parts[1].trim();

        LocalTime t = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate d;

        if (left.equalsIgnoreCase("today")) d = LocalDate.now();
        else if (left.equalsIgnoreCase("tomorrow")) d = LocalDate.now().plusDays(1);
        else d = LocalDate.now().with(DayOfWeek.valueOf(left.toUpperCase(Locale.ENGLISH)));

        return ZonedDateTime.of(d, t, ZoneId.systemDefault());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}