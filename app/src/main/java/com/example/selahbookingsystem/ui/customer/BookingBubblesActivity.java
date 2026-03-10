package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.model.ServiceItem;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.util.NailDurationCalculator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingBubblesActivity extends AppCompatActivity implements AddOnBottomSheet.OnAddOnSelectedListener {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";

    // pass to next screens
    public static final String EXTRA_SELECTED_MAP = "extra_selected_map";
    public static final String EXTRA_EST_MINS = "extra_est_mins";
    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    public static final String EXTRA_TOTAL_PRICE_CENTS = "extra_total_price_cents";

    private ChipGroup chipGroup;
    private TextView tvEstimated;

    private final LinkedHashMap<String, String> selections = new LinkedHashMap<>();

    private String providerId, providerName;
    private String currentUriStr, inspoUriStr;
    private String serviceId;
    private int totalPriceCents = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_bubbles);

        providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        currentUriStr = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        inspoUriStr = getIntent().getStringExtra(EXTRA_INSPO_URI);
        serviceId = getIntent().getStringExtra(EXTRA_SERVICE_ID);

        chipGroup = findViewById(R.id.chipGroup);
        tvEstimated = findViewById(R.id.tvEstimated);
        Button btnAddOn = findViewById(R.id.btnAddOn);
        Button btnContinue = findViewById(R.id.btnContinue);

        // Defaults
        setOrReplaceSelection("Service", "Full set");
        setOrReplaceSelection("Length", "Medium");
        setOrReplaceSelection("Shape", "Square");

        renderChips();
        updateEstimate();

        // Ensure we have a real serviceId for DB insert AND price
        ensureDefaultServiceId();

        // If serviceId came in already, load price now
        if (serviceId != null && !serviceId.trim().isEmpty()) {
            fetchServicePriceCents(serviceId);
        }

        btnAddOn.setOnClickListener(v -> {
            AddOnBottomSheet sheet = AddOnBottomSheet.newInstance(selections);
            sheet.show(getSupportFragmentManager(), "addOnSheet");
        });

        btnContinue.setOnClickListener(v -> {
            if (providerId == null || currentUriStr == null) {
                Toast.makeText(this, "Missing booking info. Go back.", Toast.LENGTH_LONG).show();
                return;
            }

            if (serviceId == null || serviceId.trim().isEmpty()) {
                Toast.makeText(this, "Missing service (service_id). Select a service first.", Toast.LENGTH_LONG).show();
                return;
            }

            // Deposits need a total price
            if (totalPriceCents <= 0) {
                Toast.makeText(this, "Price not loaded yet. Please wait a moment and try again.", Toast.LENGTH_LONG).show();
                return;
            }

            int estMins = NailDurationCalculator.estimateMinutes(selections);

            Intent i = new Intent(this, PickTimeslotActivity.class);
            i.putExtra(PickTimeslotActivity.EXTRA_PROVIDER_ID, providerId);
            i.putExtra(PickTimeslotActivity.EXTRA_PROVIDER_NAME, providerName);
            i.putExtra(PickTimeslotActivity.EXTRA_CURRENT_URI, currentUriStr);
            if (inspoUriStr != null) i.putExtra(PickTimeslotActivity.EXTRA_INSPO_URI, inspoUriStr);

            i.putExtra(PickTimeslotActivity.EXTRA_SELECTED_MAP, serializeSelections(selections));
            i.putExtra(PickTimeslotActivity.EXTRA_EST_MINS, estMins);
            i.putExtra(PickTimeslotActivity.EXTRA_SERVICE_ID, serviceId);

            // pass price forward
            i.putExtra(PickTimeslotActivity.EXTRA_TOTAL_PRICE_CENTS, totalPriceCents);

            startActivity(i);
        });
    }

    private void setOrReplaceSelection(String category, String value) {
        selections.put(category, value);
    }

    private void renderChips() {
        chipGroup.removeAllViews();

        for (Map.Entry<String, String> entry : selections.entrySet()) {
            String category = entry.getKey();
            String value = entry.getValue();

            Chip chip = new Chip(this);
            chip.setText(category + ": " + value);
            chip.setCloseIconVisible(true);
            chip.setCheckable(false);

            chip.setOnCloseIconClickListener(v -> {
                selections.remove(category);

                if ("Service".equals(category)) {
                    serviceId = null;
                    totalPriceCents = 0;
                }

                renderChips();
                updateEstimate();
            });

            chipGroup.addView(chip);
        }
    }

    private void updateEstimate() {
        int mins = NailDurationCalculator.estimateMinutes(selections);
        tvEstimated.setText("Estimated time: " + NailDurationCalculator.pretty(mins));
    }

    @Override
    public void onAddOnSelected(String category,
                                String value,
                                @Nullable String designPrompt,
                                @Nullable String selectedServiceId) {

        if (category == null || value == null) return;

        if ("Service".equals(category)) {
            serviceId = selectedServiceId;
            totalPriceCents = 0;

            setOrReplaceSelection("Service", value);

            // load price for selected service
            fetchServicePriceCents(serviceId);

        } else if ("Design".equals(category) && designPrompt != null && !designPrompt.trim().isEmpty()) {
            setOrReplaceSelection("Design", designPrompt.trim());
        } else {
            setOrReplaceSelection(category, value);
        }

        renderChips();
        updateEstimate();
    }

    private void fetchServicePriceCents(String serviceId) {
        if (serviceId == null || serviceId.trim().isEmpty()) return;

        ApiClient.services()
                .getServiceById("eq." + serviceId, "id,name,base_price_cents")
                .enqueue(new Callback<List<ServiceItem>>() {
                    @Override
                    public void onResponse(Call<List<ServiceItem>> call, Response<List<ServiceItem>> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            Log.e("PRICE", "Failed to fetch price: HTTP " + response.code());
                            return;
                        }

                        ServiceItem s = response.body().get(0);
                        if (s != null && s.base_price_cents != null) {
                            totalPriceCents = s.base_price_cents;
                            Log.d("PRICE", "Loaded totalPriceCents=" + totalPriceCents + " for " + s.name);
                        } else {
                            Log.e("PRICE", "base_price_cents missing for serviceId=" + serviceId);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ServiceItem>> call, Throwable t) {
                        Log.e("PRICE", "Network error fetching price", t);
                    }
                });
    }

    public static String serializeSelections(LinkedHashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue().replace(";", ",")).append(";");
        }
        return sb.toString();
    }

    public static LinkedHashMap<String, String> deserializeSelections(String raw) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (raw == null) return out;
        String[] parts = raw.split(";");
        for (String p : parts) {
            if (p.trim().isEmpty()) continue;
            int idx = p.indexOf('=');
            if (idx <= 0) continue;
            String k = p.substring(0, idx);
            String v = p.substring(idx + 1);
            out.put(k, v);
        }
        return out;
    }

    private void ensureDefaultServiceId() {
        if (serviceId != null && !serviceId.trim().isEmpty()) return;

        String defaultServiceName = selections.get("Service");
        if (defaultServiceName == null || defaultServiceName.trim().isEmpty()) return;

        ApiClient.services()
                .listServices("id,name,base_price_cents")
                .enqueue(new Callback<List<ServiceItem>>() {
                    @Override
                    public void onResponse(Call<List<ServiceItem>> call, Response<List<ServiceItem>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e("SERVICES", "Failed to fetch services: HTTP " + response.code());
                            return;
                        }

                        for (ServiceItem s : response.body()) {
                            if (s != null && s.id != null && s.name != null
                                    && s.name.equalsIgnoreCase(defaultServiceName)) {

                                serviceId = s.id;

                                if (s.base_price_cents != null) {
                                    totalPriceCents = s.base_price_cents;
                                    Log.d("PRICE", "Default price loaded: " + totalPriceCents);
                                } else {
                                    fetchServicePriceCents(serviceId);
                                }

                                Log.d("SERVICES", "Default serviceId set: " + serviceId + " for " + defaultServiceName);
                                return;
                            }
                        }

                        Log.e("SERVICES", "No matching service found for default: " + defaultServiceName);
                    }

                    @Override
                    public void onFailure(Call<List<ServiceItem>> call, Throwable t) {
                        Log.e("SERVICES", "Network error fetching services", t);
                    }
                });
    }
}