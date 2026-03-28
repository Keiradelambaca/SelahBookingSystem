package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.SPServicePricingAdapter;
import com.example.selahbookingsystem.data.dto.AppServiceDto;
import com.example.selahbookingsystem.data.dto.ProviderServicePricingDto;
import com.example.selahbookingsystem.data.model.ProviderServiceConfigItem;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPServicesActivity extends SPBaseActivity {

    @Override protected int getLayoutResourceId() { return R.layout.activity_sp_services; }
    @Override protected int getSelectedNavItemId() { return R.id.nav_sp_scheduling; }

    private ProgressBar progress;
    private RecyclerView recycler;
    private View emptyState;

    private SPServicePricingAdapter adapter;
    private SupabaseRestService rest;

    private final List<AppServiceDto> appServices = new ArrayList<>();
    private final List<ProviderServicePricingDto> providerPricing = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.init(getApplicationContext());
        rest = ApiClient.get().create(SupabaseRestService.class);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        recycler = findViewById(R.id.recyclerServices);
        emptyState = findViewById(R.id.emptyState);

        adapter = new SPServicePricingAdapter(this::showEditServiceDialog);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAll();
    }

    private void loadAll() {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) {
            Toast.makeText(this, "No provider session found.", Toast.LENGTH_SHORT).show();
            adapter.submitList(null);
            showEmpty(true);
            return;
        }

        setLoading(true);
        loadAppServicesThenPricing(providerId);
    }

    private void loadAppServicesThenPricing(String providerId) {
        rest.listAppServices(
                "eq.true",
                "id,code,name,category,is_active",
                "category.asc,name.asc"
        ).enqueue(new Callback<List<AppServiceDto>>() {
            @Override
            public void onResponse(Call<List<AppServiceDto>> call, Response<List<AppServiceDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setLoading(false);
                    Toast.makeText(SPServicesActivity.this, "Failed to load app services", Toast.LENGTH_SHORT).show();
                    adapter.submitList(null);
                    showEmpty(true);
                    return;
                }

                appServices.clear();
                appServices.addAll(response.body());

                loadProviderPricing(providerId);
            }

            @Override
            public void onFailure(Call<List<AppServiceDto>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(SPServicesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                adapter.submitList(null);
                showEmpty(true);
            }
        });
    }

    private void loadProviderPricing(String providerId) {
        rest.getProviderServicePricing(
                "eq." + providerId,
                "*",
                "service_category.asc,service_name.asc"
        ).enqueue(new Callback<List<ProviderServicePricingDto>>() {
            @Override
            public void onResponse(Call<List<ProviderServicePricingDto>> call, Response<List<ProviderServicePricingDto>> response) {
                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SPServicesActivity.this, "Failed to load provider pricing", Toast.LENGTH_SHORT).show();
                    adapter.submitList(null);
                    showEmpty(true);
                    return;
                }

                providerPricing.clear();
                providerPricing.addAll(response.body());

                List<ProviderServiceConfigItem> merged = mergeCatalogueWithPricing(appServices, providerPricing);
                adapter.submitList(merged);
                showEmpty(merged.isEmpty());
            }

            @Override
            public void onFailure(Call<List<ProviderServicePricingDto>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(SPServicesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                adapter.submitList(null);
                showEmpty(true);
            }
        });
    }

    private List<ProviderServiceConfigItem> mergeCatalogueWithPricing(
            List<AppServiceDto> catalogue,
            List<ProviderServicePricingDto> pricing
    ) {
        Map<String, ProviderServicePricingDto> pricingByAppServiceId = new LinkedHashMap<>();
        for (ProviderServicePricingDto row : pricing) {
            if (row != null && row.app_service_id != null) {
                pricingByAppServiceId.put(row.app_service_id, row);
            }
        }

        List<ProviderServiceConfigItem> out = new ArrayList<>();

        for (AppServiceDto app : catalogue) {
            if (app == null || app.id == null) continue;

            ProviderServicePricingDto existing = pricingByAppServiceId.get(app.id);

            ProviderServiceConfigItem item = new ProviderServiceConfigItem();
            item.app_service_id = app.id;
            item.service_code = app.code;
            item.service_name = app.name;
            item.service_category = app.category;

            if (existing != null) {
                item.is_offered = Boolean.TRUE.equals(existing.is_offered);
                item.price_cents = existing.price_cents != null ? existing.price_cents : 0;
                item.duration_mins = existing.duration_mins != null ? existing.duration_mins : defaultDurationForCategory(app.category);
            } else {
                item.is_offered = false;
                item.price_cents = 0;
                item.duration_mins = defaultDurationForCategory(app.category);
            }

            out.add(item);
        }

        return out;
    }

    private int defaultDurationForCategory(String category) {
        if ("base".equals(category)) return 60;
        if ("addon".equals(category)) return 15;
        if ("complexity".equals(category)) return 20;
        return 15;
    }

    private void showEditServiceDialog(ProviderServiceConfigItem item) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_pricing, null, false);

        SwitchCompat switchOffered = view.findViewById(R.id.switchOffered);
        TextInputEditText etPrice = view.findViewById(R.id.etPrice);
        TextInputEditText etMins = view.findViewById(R.id.etMins);

        switchOffered.setChecked(item.is_offered);
        etPrice.setText(formatEurosInput(item.price_cents));
        etMins.setText(String.valueOf(item.duration_mins));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(item.service_name)
                .setView(view)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                boolean isOffered = switchOffered.isChecked();
                int priceCents = eurosToCents(safe(etPrice.getText()));
                int mins = parseInt(safe(etMins.getText()));

                if (isOffered && mins <= 0) {
                    etMins.setError("Enter minutes");
                    return;
                }

                if (!isOffered) {
                    priceCents = 0;
                    if (mins <= 0) mins = defaultDurationForCategory(item.service_category);
                }

                saveProviderPricing(item, isOffered, priceCents, mins, dialog);
            });
        });

        dialog.show();
    }

    private void saveProviderPricing(
            ProviderServiceConfigItem item,
            boolean isOffered,
            int priceCents,
            int mins,
            AlertDialog dialog
    ) {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) {
            Toast.makeText(this, "No provider session found.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, Object> row = new HashMap<>();
        row.put("provider_id", providerId);
        row.put("app_service_id", item.app_service_id);
        row.put("is_offered", isOffered);
        row.put("price_cents", priceCents);
        row.put("duration_mins", mins);

        List<Map<String, Object>> body = new ArrayList<>();
        body.add(row);

        rest.upsertProviderServicePricing("provider_id,app_service_id", body)
                .enqueue(new Callback<List<ProviderServicePricingDto>>() {
                    @Override
                    public void onResponse(Call<List<ProviderServicePricingDto>> call, Response<List<ProviderServicePricingDto>> response) {
                        setLoading(false);

                        if (!response.isSuccessful()) {
                            Toast.makeText(SPServicesActivity.this, "Failed to save service settings", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(SPServicesActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadAll();
                    }

                    @Override
                    public void onFailure(Call<List<ProviderServicePricingDto>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(SPServicesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception ignored) { return 0; }
    }

    private static int eurosToCents(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        String normalized = s.trim().replace(",", ".");
        try {
            double eur = Double.parseDouble(normalized);
            return (int) Math.round(eur * 100.0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String formatEurosInput(int cents) {
        return String.format(java.util.Locale.getDefault(), "%.2f", cents / 100.0);
    }
}