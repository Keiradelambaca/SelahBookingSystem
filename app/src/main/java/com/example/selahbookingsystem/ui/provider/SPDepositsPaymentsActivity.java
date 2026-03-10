package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.ProviderSettingsDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPDepositsPaymentsActivity extends SPBaseActivity {

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_sp_deposits_payment;
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_sp_scheduling;
    } // stays highlighted in Scheduling tab

    private SwitchMaterial swEnableDeposits;
    private EditText etDepositPercent;
    private TextView tvExample;
    private MaterialButton btnSave;

    private SupabaseRestService rest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rest = ApiClient.get().create(SupabaseRestService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        swEnableDeposits = findViewById(R.id.swEnableDeposits);
        etDepositPercent = findViewById(R.id.etDepositPercent);
        tvExample = findViewById(R.id.tvExample);
        btnSave = findViewById(R.id.btnSaveDepositSettings);

        swEnableDeposits.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etDepositPercent.setEnabled(isChecked);
            if (!isChecked) etDepositPercent.setText("0");
            updateExample();
        });

        etDepositPercent.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateExample();
        });

        btnSave.setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void setLoading(boolean loading) {
        btnSave.setEnabled(!loading);
        btnSave.setText(loading ? "Saving..." : "Save");
    }

    private void loadSettings() {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // GET provider_settings?provider_id=eq.<id>&select=*
        rest.getProviderSettings("eq." + providerId, "*").enqueue(new Callback<List<ProviderSettingsDto>>() {
            @Override
            public void onResponse(Call<List<ProviderSettingsDto>> call, Response<List<ProviderSettingsDto>> resp) {
                if (!resp.isSuccessful()) {
                    // If table exists but row doesn't, PostgREST often returns 200 with [].
                    Toast.makeText(SPDepositsPaymentsActivity.this, "Could not load settings", Toast.LENGTH_SHORT).show();
                    applyDefaults();
                    return;
                }

                List<ProviderSettingsDto> list = resp.body();
                if (list == null || list.isEmpty()) {
                    applyDefaults();
                    return;
                }

                ProviderSettingsDto s = list.get(0);
                swEnableDeposits.setChecked(s.deposits_enabled);
                etDepositPercent.setText(String.valueOf(s.deposit_percent));
                etDepositPercent.setEnabled(s.deposits_enabled);
                updateExample();
            }

            @Override
            public void onFailure(Call<List<ProviderSettingsDto>> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(SPDepositsPaymentsActivity.this,
                        "Network error: " + t.getClass().getSimpleName(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyDefaults() {
        swEnableDeposits.setChecked(false);
        etDepositPercent.setText("0");
        etDepositPercent.setEnabled(false);
        updateExample();
    }

    private int readPercent() {
        String s = etDepositPercent.getText() == null ? "" : etDepositPercent.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return 0;

        try {
            int p = Integer.parseInt(s);
            if (p < 0) p = 0;
            if (p > 100) p = 100;
            return p;
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateExample() {
        boolean enabled = swEnableDeposits.isChecked();
        int p = enabled ? readPercent() : 0;

        // Example using €100.00
        int totalCents = 10000;
        int depositCents = (int) Math.round(totalCents * (p / 100.0));
        double depositEuro = depositCents / 100.0;

        if (!enabled) {
            tvExample.setText("Deposits are disabled. Clients can confirm bookings without paying upfront.");
        } else {
            tvExample.setText(String.format(Locale.getDefault(),
                    "Example: If the service total is €100.00, a %d%% deposit is €%.2f. The deposit must be paid before the booking is confirmed.",
                    p, depositEuro));
        }
    }

    private void saveSettings() {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) return;

        boolean enabled = swEnableDeposits.isChecked();
        int percent = enabled ? readPercent() : 0;

        // Validate when enabled
        if (enabled && percent <= 0) {
            Toast.makeText(this, "Deposit % must be at least 1 when enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, Object> body = new HashMap<>();
        body.put("provider_id", providerId);
        body.put("deposits_enabled", enabled);
        body.put("deposit_percent", percent);

        rest.upsertProviderSettings(body).enqueue(new Callback<List<ProviderSettingsDto>>() {
            @Override
            public void onResponse(Call<List<ProviderSettingsDto>> call, Response<List<ProviderSettingsDto>> resp) {
                setLoading(false);
                if (!resp.isSuccessful()) {
                    Toast.makeText(SPDepositsPaymentsActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(SPDepositsPaymentsActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                updateExample();
            }

            @Override
            public void onFailure(Call<List<ProviderSettingsDto>> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(SPDepositsPaymentsActivity.this,
                        "Network error: " + t.getClass().getSimpleName(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}