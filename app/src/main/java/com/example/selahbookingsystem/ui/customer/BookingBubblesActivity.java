package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.util.NailDurationCalculator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.LinkedHashMap;
import java.util.Map;

import android.util.Log;

import com.example.selahbookingsystem.data.model.ServiceItem;
import com.example.selahbookingsystem.network.api.ApiClient;

import java.util.List;

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

    private ChipGroup chipGroup;
    private TextView tvEstimated;

    // category -> value
    private final LinkedHashMap<String, String> selections = new LinkedHashMap<>();

    private String providerId, providerName;
    private String currentUriStr, inspoUriStr;
    private String serviceId;

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

        // Phase 1 defaults (can tweak)
        setOrReplaceSelection("Service", "Full set");
        setOrReplaceSelection("Length", "Medium");
        setOrReplaceSelection("Shape", "Square");

        ensureDefaultServiceId();

        renderChips();
        updateEstimate();

        btnAddOn.setOnClickListener(v -> {
            AddOnBottomSheet sheet = AddOnBottomSheet.newInstance(selections);
            sheet.show(getSupportFragmentManager(), "addOnSheet");
        });

        btnContinue.setOnClickListener(v -> {
            if (providerId == null || currentUriStr == null) {
                Toast.makeText(this, "Missing booking info. Go back.", Toast.LENGTH_LONG).show();
                return;
            }

            // If you haven't wired real service selection yet, this will still be null.
            // You NEED a real uuid from the 'services' table, otherwise insert will fail.
            if (serviceId == null || serviceId.trim().isEmpty()) {
                Toast.makeText(this, "Missing service (service_id). Select a service first.", Toast.LENGTH_LONG).show();
                return;
            }

            int estMins = NailDurationCalculator.estimateMinutes(selections);

            Intent i = new Intent(this, PickTimeslotActivity.class);
            i.putExtra(PickTimeslotActivity.EXTRA_PROVIDER_ID, providerId);
            i.putExtra(PickTimeslotActivity.EXTRA_PROVIDER_NAME, providerName);
            i.putExtra(PickTimeslotActivity.EXTRA_CURRENT_URI, currentUriStr);
            if (inspoUriStr != null) i.putExtra(PickTimeslotActivity.EXTRA_INSPO_URI, inspoUriStr);

            // selections + estimate
            i.putExtra(PickTimeslotActivity.EXTRA_SELECTED_MAP, serializeSelections(selections));
            i.putExtra(PickTimeslotActivity.EXTRA_EST_MINS, estMins);
            i.putExtra(PickTimeslotActivity.EXTRA_SERVICE_ID, serviceId);

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

                // If user removes "Service" chip, also clear serviceId (so we force re-select)
                if ("Service".equals(category)) {
                    serviceId = null;
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
            // Store real UUID for DB insert
            serviceId = selectedServiceId;

            // Show readable name in chip
            setOrReplaceSelection("Service", value);

        } else if ("Design".equals(category) && designPrompt != null && !designPrompt.trim().isEmpty()) {
            setOrReplaceSelection("Design", designPrompt.trim());

        } else {
            setOrReplaceSelection(category, value);
        }

        renderChips();
        updateEstimate();
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

        // This must match a real row in your services table
        String defaultServiceName = selections.get("Service"); // e.g. "Full set"
        if (defaultServiceName == null || defaultServiceName.trim().isEmpty()) return;

        // Supabase filter: name=eq.Full set
        ApiClient.services()
                .listServices("id,name") // must include the name column you filter by
                .enqueue(new Callback<List<ServiceItem>>() {
                    @Override
                    public void onResponse(Call<List<ServiceItem>> call, Response<List<ServiceItem>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e("SERVICES", "Failed to fetch services: " + response.code());
                            return;
                        }

                        // Find matching name (case-insensitive)
                        for (ServiceItem s : response.body()) {
                            if (s != null && s.id != null && s.name != null
                                    && s.name.equalsIgnoreCase(defaultServiceName)) {
                                serviceId = s.id;
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