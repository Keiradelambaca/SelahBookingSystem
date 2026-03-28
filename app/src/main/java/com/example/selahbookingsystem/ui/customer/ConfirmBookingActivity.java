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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    public static final String EXTRA_TOTAL_PRICE_CENTS = "extra_total_price_cents";

    private SupabaseRestService api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_booking);

        ApiClient.init(getApplicationContext());
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

            fetchClientNameAndContinue(
                    clientId,
                    providerId,
                    providerName,
                    currentUrl,
                    inspoUrl,
                    selections,
                    estMins,
                    totalPriceCents,
                    startZdt,
                    endZdt,
                    btnConfirm
            );
        });
    }

    private void fetchClientNameAndContinue(
            String clientId,
            String providerId,
            String providerName,
            String currentUrl,
            String inspoUrl,
            LinkedHashMap<String, String> selections,
            int estMins,
            int totalPriceCents,
            ZonedDateTime startZdt,
            ZonedDateTime endZdt,
            Button btnConfirm
    ) {
        api.getProfile("eq." + clientId, "id,full_name")
                .enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
                    @Override
                    public void onResponse(Call<List<SupabaseRestService.ProfileDto>> call,
                                           Response<List<SupabaseRestService.ProfileDto>> profileResp) {

                        String clientName = "Client";
                        if (profileResp.isSuccessful()
                                && profileResp.body() != null
                                && !profileResp.body().isEmpty()
                                && profileResp.body().get(0).full_name != null
                                && !profileResp.body().get(0).full_name.trim().isEmpty()) {
                            clientName = profileResp.body().get(0).full_name.trim();
                        }

                        continueCreateBooking(
                                clientId,
                                clientName,
                                providerId,
                                providerName,
                                currentUrl,
                                inspoUrl,
                                selections,
                                estMins,
                                totalPriceCents,
                                startZdt,
                                endZdt,
                                btnConfirm
                        );
                    }

                    @Override
                    public void onFailure(Call<List<SupabaseRestService.ProfileDto>> call, Throwable t) {
                        continueCreateBooking(
                                clientId,
                                "Client",
                                providerId,
                                providerName,
                                currentUrl,
                                inspoUrl,
                                selections,
                                estMins,
                                totalPriceCents,
                                startZdt,
                                endZdt,
                                btnConfirm
                        );
                    }
                });
    }

    private void continueCreateBooking(
            String clientId,
            String clientName,
            String providerId,
            String providerName,
            String currentUrl,
            String inspoUrl,
            LinkedHashMap<String, String> selections,
            int estMins,
            int totalPriceCents,
            ZonedDateTime startZdt,
            ZonedDateTime endZdt,
            Button btnConfirm
    ) {
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

                        BookingDto body = new BookingDto();
                        body.client_id = clientId;
                        body.provider_id = providerId;
                        body.provider_name = providerName;
                        body.start_time = startZdt.toInstant().toString();
                        body.end_time = endZdt.toInstant().toString();
                        body.duration_mins = estMins;
                        body.details_json = buildBookingDetailsJson(
                                selections,
                                estMins,
                                totalPriceCents,
                                currentUrl,
                                inspoUrl,
                                clientName,
                                providerName
                        );
                        body.current_photo_url = currentUrl;
                        body.inspo_photo_url = inspoUrl;
                        body.total_price_cents = totalPriceCents;

                        if (depositsEnabled) {
                            body.status = "payment_pending";
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
    }

    private Map<String, Object> buildBookingDetailsJson(
            LinkedHashMap<String, String> selections,
            int estMins,
            int totalPriceCents,
            @Nullable String currentUrl,
            @Nullable String inspoUrl,
            @Nullable String clientName,
            @Nullable String providerName
    ) {
        Map<String, Object> details = new LinkedHashMap<>();

        details.put("client_name", safe(clientName));
        details.put("provider_name", safe(providerName));

        if (!TextUtils.isEmpty(currentUrl)) {
            details.put("current_photo_url", currentUrl);
        }
        if (!TextUtils.isEmpty(inspoUrl)) {
            details.put("inspo_photo_url", inspoUrl);
        }

        details.put("display_selections", selections);

        Map<String, Object> structuredSelection = new LinkedHashMap<>();
        structuredSelection.put("base_service_code", extractBaseServiceCode(selections));
        structuredSelection.put("addon_codes", extractAddonCodes(selections));
        structuredSelection.put("design_level", extractDesignLevel(selections));
        structuredSelection.put("shape", extractSimpleNormalizedValue(selections.get("Shape")));
        structuredSelection.put("length", extractSimpleNormalizedValue(selections.get("Length")));

        details.put("structured_selection", structuredSelection);

        Map<String, Object> pricingSnapshot = new LinkedHashMap<>();
        pricingSnapshot.put("total_price_cents", totalPriceCents);
        pricingSnapshot.put("total_duration_mins", estMins);
        pricingSnapshot.put("included_service_codes", buildIncludedServiceCodes(selections));

        details.put("pricing_snapshot", pricingSnapshot);

        return details;
    }

    private String extractBaseServiceCode(LinkedHashMap<String, String> selections) {
        return codeFromFriendlyServiceName(selections != null ? selections.get("Service") : null);
    }

    private List<String> extractAddonCodes(LinkedHashMap<String, String> selections) {
        List<String> addonCodes = new ArrayList<>();
        if (selections == null) return addonCodes;

        for (Map.Entry<String, String> entry : selections.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key != null && key.startsWith("Add-on ")) {
                String code = codeFromFriendlyServiceName(value);
                if (!TextUtils.isEmpty(code)) {
                    addonCodes.add(code);
                }
            }
        }

        return addonCodes;
    }

    private String extractDesignLevel(LinkedHashMap<String, String> selections) {
        if (selections == null) return "low";

        String raw = selections.get("Design Level");
        if (TextUtils.isEmpty(raw)) return "low";

        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
        if ("medium".equals(normalized) || "high".equals(normalized) || "low".equals(normalized)) {
            return normalized;
        }

        return "low";
    }

    private String extractSimpleNormalizedValue(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) return null;
        return raw.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private List<String> buildIncludedServiceCodes(LinkedHashMap<String, String> selections) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();

        String baseService = extractBaseServiceCode(selections);
        if (!TextUtils.isEmpty(baseService)) {
            codes.add(baseService);
        }

        for (String addon : extractAddonCodes(selections)) {
            if (!TextUtils.isEmpty(addon)) {
                codes.add(addon);
            }
        }

        String designLevel = extractDesignLevel(selections);
        if ("medium".equals(designLevel)) {
            codes.add("design_medium");
        } else if ("high".equals(designLevel)) {
            codes.add("design_high");
        }

        return new ArrayList<>(codes);
    }

    private void triggerConfirmedEmailSafely(String bookingId) {
        if (TextUtils.isEmpty(bookingId)) return;

        Map<String, Object> payload = new LinkedHashMap<>();
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
        sb.append("Duration: ").append(prettyDuration(mins)).append("\n");

        if (totalPriceCents > 0) {
            sb.append("Total: ")
                    .append(String.format(Locale.getDefault(), "€%.2f", totalPriceCents / 100.0))
                    .append("\n");
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
        String[] parts = slot.split("•");
        if (parts.length < 2) throw new IllegalArgumentException("Bad slot format");

        String dateStr = parts[0].trim();
        String timeStr = parts[1].trim();

        LocalDate d = LocalDate.parse(dateStr);
        LocalTime t = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        return ZonedDateTime.of(d, t, ZoneId.of("Europe/Dublin"));
    }

    private String codeFromFriendlyServiceName(@Nullable String name) {
        if (name == null) return null;

        String normalized = name.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "biab overlay": return "biab_overlay";
            case "biab infill": return "biab_infill";
            case "gel polish": return "gel_polish";
            case "gel extensions": return "gel_extensions";
            case "acrylic extensions": return "acrylic_extensions";
            case "acrylic infill": return "acrylic_infill";
            case "removal": return "removal";
            case "french tip": return "french_tip";
            case "chrome": return "chrome";
            case "ombre": return "ombre";
            case "gems / charms": return "gems_charms";
            case "hand drawn art": return "hand_drawn_art";
            case "3d art": return "three_d_art";
            case "glitter": return "glitter";
            case "extra length": return "extra_length";
            case "medium design": return "design_medium";
            case "high design": return "design_high";
            default:
                return normalized.replace(" ", "_");
        }
    }

    private String prettyDuration(int mins) {
        if (mins <= 0) return "0 mins";

        int hours = mins / 60;
        int remaining = mins % 60;

        if (hours > 0) {
            return remaining > 0 ? hours + "h " + remaining + "m" : hours + "h";
        }
        return mins + " mins";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
