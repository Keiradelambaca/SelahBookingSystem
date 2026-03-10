package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.SPServicesAdapter;
import com.example.selahbookingsystem.data.dto.ServiceDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
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
    private ExtendedFloatingActionButton fabAdd;

    private SPServicesAdapter adapter;
    private SupabaseRestService rest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        recycler = findViewById(R.id.recyclerServices);
        emptyState = findViewById(R.id.emptyState);
        fabAdd = findViewById(R.id.fabAddService);

        rest = ApiClient.get().create(SupabaseRestService.class);

        adapter = new SPServicesAdapter(service -> {
            Toast.makeText(
                    this,
                    "Edit bubbles next: " + (service.name == null ? "" : service.name),
                    Toast.LENGTH_SHORT
            ).show();
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddServiceDialog());
        }

        loadServices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadServices();
    }

    private void loadServices() {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) {
            Toast.makeText(this, "No provider session found.", Toast.LENGTH_SHORT).show();
            adapter.submitList(null);
            showEmpty(true);
            return;
        }

        setLoading(true);

        rest.listServicesForProvider(
                "eq." + providerId,
                "eq.true",
                "created_at.asc",
                "id,provider_id,name,base_price_cents,base_duration_mins,is_active"
        ).enqueue(new Callback<List<ServiceDto>>() {
            @Override
            public void onResponse(Call<List<ServiceDto>> call, Response<List<ServiceDto>> resp) {
                setLoading(false);

                if (!resp.isSuccessful()) {
                    Toast.makeText(SPServicesActivity.this, "Failed to load services", Toast.LENGTH_SHORT).show();
                    adapter.submitList(null);
                    showEmpty(true);
                    return;
                }

                List<ServiceDto> data = resp.body();
                adapter.submitList(data);
                showEmpty(data == null || data.isEmpty());
            }

            @Override
            public void onFailure(Call<List<ServiceDto>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(SPServicesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                adapter.submitList(null);
                showEmpty(true);
            }
        });
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showAddServiceDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_service, null, false);

        TextInputEditText etName  = view.findViewById(R.id.etServiceName);
        TextInputEditText etPrice = view.findViewById(R.id.etBasePrice);
        TextInputEditText etMins  = view.findViewById(R.id.etBaseMins);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
                this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog
        );

        androidx.appcompat.app.AlertDialog dialog = builder
                .setTitle("Add service")
                .setView(view)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button saveBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {

                String name = safe(etName == null ? null : etName.getText());
                String priceStr = safe(etPrice == null ? null : etPrice.getText());
                String minsStr  = safe(etMins == null ? null : etMins.getText());

                if (TextUtils.isEmpty(name)) {
                    if (etName != null) etName.setError("Required");
                    return;
                }

                int priceCents = eurosToCents(priceStr);
                int mins = parseInt(minsStr);
                if (mins <= 0) mins = 60;

                createService(name, priceCents, mins);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void createService(String name, int basePriceCents, int baseMins) {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) return;

        setLoading(true);

        Map<String, Object> body = new HashMap<>();
        body.put("provider_id", providerId);
        body.put("name", name);
        body.put("base_price_cents", basePriceCents);
        body.put("base_duration_mins", baseMins);
        body.put("is_active", true);

        rest.createService(body).enqueue(new Callback<List<ServiceDto>>() {
            @Override
            public void onResponse(Call<List<ServiceDto>> call, Response<List<ServiceDto>> resp) {
                setLoading(false);

                if (!resp.isSuccessful()) {
                    Toast.makeText(SPServicesActivity.this, "Failed to create service", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(SPServicesActivity.this, "Service added", Toast.LENGTH_SHORT).show();

                // IMPORTANT: reload from DB so it persists and always matches the server
                loadServices();
            }

            @Override
            public void onFailure(Call<List<ServiceDto>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(SPServicesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (fabAdd != null) fabAdd.setEnabled(!loading);
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
}