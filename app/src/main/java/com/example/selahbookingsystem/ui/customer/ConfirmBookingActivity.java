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
import com.example.selahbookingsystem.data.dto.ProviderSettingsDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.util.NailDurationCalculator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // total price for booking (cents)
    public static final String EXTRA_TOTAL_PRICE_CENTS = "extra_total_price_cents";

    private SupabaseRestService api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_booking);

        api = ApiClient.get().create(SupabaseRestService.class);

        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        String currentUrl = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        String inspoUrl = getIntent().getStringExtra(EXTRA_INSPO_URI);
        String rawMap = getIntent().getStringExtra(EXTRA_SELECTED_MAP);
        int estMins = getIntent().getIntExtra(EXTRA_EST_MINS, 60);
        String slotLabel = getIntent().getStringExtra(EXTRA_SELECTED_SLOT);
        int totalPriceCents = getIntent().getIntExtra(EXTRA_TOTAL_PRICE_CENTS, 0);

        LinkedHashMap<String, String> selections =
                BookingBubblesActivity.deserializeSelections(rawMap);

        TextView tvSummary = findViewById(R.id.tvSummary);
        tvSummary.setText(buildSummary(providerName, slotLabel, estMins, selections, totalPriceCents));

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

            // 1) fetch provider deposit settings
            api.getProviderSettings("eq." + providerId, "deposits_enabled,deposit_percent")
                    .enqueue(new Callback<List<ProviderSettingsDto>>() {
                        @Override
                        public void onResponse(Call<List<ProviderSettingsDto>> call, Response<List<ProviderSettingsDto>> resp) {

                            ProviderSettingsDto settings = null;
                            if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                                settings = resp.body().get(0);
                            }

                            boolean depositsEnabled =
                                    settings != null
                                            && Boolean.TRUE.equals(settings.deposits_enabled)
                                            && settings.deposit_percent != null
                                            && settings.deposit_percent > 0;

                            int depositPercent = depositsEnabled ? settings.deposit_percent : 0;

                            // If deposits enabled but no price yet, block (otherwise you'd charge €0)
                            if (depositsEnabled && totalPriceCents <= 0) {
                                toast("This provider requires a deposit, but total price is missing.");
                                btnConfirm.setEnabled(true);
                                return;
                            }

                            int depositAmountCents = depositsEnabled
                                    ? (int) Math.round(totalPriceCents * (depositPercent / 100.0))
                                    : 0;

                            if (depositsEnabled && depositAmountCents <= 0) {
                                toast("Deposit amount is invalid. Please go back and try again.");
                                btnConfirm.setEnabled(true);
                                return;
                            }

                            // 2) create booking (confirmed if no deposit; payment_pending if deposit required)
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

                            // requires matching columns in your DB/DTO
                            body.total_price_cents = totalPriceCents;

                            if (depositsEnabled) {
                                body.status = "payment_pending";            // align with your DB status
                                body.deposit_required = true;
                                body.deposit_percent = depositPercent;
                                body.deposit_amount_cents = depositAmountCents;
                                body.payment_status = "requires_payment";
                            } else {
                                body.status = "confirmed";
                                body.deposit_required = false;
                                body.payment_status = "not_required";
                            }

                            api.createBooking(body).enqueue(new Callback<List<BookingDto>>() {
                                @Override
                                public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                        String bookingId = response.body().get(0).id;

                                        if (!depositsEnabled) {
                                            triggerConfirmedEmailSafely(bookingId);

                                            Intent i = new Intent(ConfirmBookingActivity.this, BookingConfirmedActivity.class);
                                            i.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID, bookingId);
                                            startActivity(i);
                                            finish();
                                        } else {
                                            // Deposit flow
                                            Intent i = new Intent(ConfirmBookingActivity.this, DepositPaymentActivity.class);
                                            i.putExtra(DepositPaymentActivity.EXTRA_BOOKING_ID, bookingId);
                                            i.putExtra(DepositPaymentActivity.EXTRA_DEPOSIT_CENTS, depositAmountCents);
                                            i.putExtra(DepositPaymentActivity.EXTRA_TOTAL_CENTS, totalPriceCents);
                                            i.putExtra(DepositPaymentActivity.EXTRA_DEPOSIT_PERCENT, depositPercent);
                                            i.putExtra(DepositPaymentActivity.EXTRA_PROVIDER_NAME, providerName);
                                            startActivity(i);
                                            finish();
                                        }
                                    } else {
                                        toast("Booking failed (" + response.code() + ")");
                                        btnConfirm.setEnabled(true);
                                    }
                                }

                                @Override
                                public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                                    toast("Network error");
                                    btnConfirm.setEnabled(true);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<List<ProviderSettingsDto>> call, Throwable t) {
                            toast("Network error");
                            btnConfirm.setEnabled(true);
                        }
                    });
        });
    }

    private void triggerConfirmedEmailSafely(String bookingId) {
        if (TextUtils.isEmpty(bookingId)) return;

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("booking_id", bookingId);

        api.fnBookingConfirmedEmail(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) { }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { }
        });
    }

    private String buildSummary(String provider, String slot, int mins,
                                LinkedHashMap<String, String> map,
                                int totalPriceCents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Provider: ").append(safe(provider)).append("\n");
        sb.append("Time: ").append(safe(slot)).append("\n");
        sb.append("Duration: ").append(NailDurationCalculator.pretty(mins)).append("\n");
        if (totalPriceCents > 0) {
            sb.append("Total: €").append(String.format(Locale.getDefault(), "%.2f", totalPriceCents / 100.0)).append("\n");
        }
        sb.append("\n");
        if (map != null) {
            for (String k : map.keySet()) {
                sb.append("• ").append(k).append(": ").append(map.get(k)).append("\n");
            }
        }
        return sb.toString();
    }

    private ZonedDateTime parseSlot(String slot) {
        // Expected: "YYYY-MM-DD • HH:mm"
        String[] parts = slot.split("•");
        if (parts.length < 2) throw new IllegalArgumentException("Bad slot format");

        String dateStr = parts[0].trim();
        String timeStr = parts[1].trim();

        LocalDate d = LocalDate.parse(dateStr);
        LocalTime t = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        return ZonedDateTime.of(d, t, ZoneId.of("Europe/Dublin"));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}