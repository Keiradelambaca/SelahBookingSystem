package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.WeeklyAvailabilityAdapter;
import com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPAvailabilityActivity extends AppCompatActivity {

    private static final String TAG = "SP_AVAIL";

    private TextView tvSelectedDate;
    private RecyclerView rvWeekly;
    private MaterialButton btnSave;

    private final List<WeeklyAvailabilityDto> weekly = new ArrayList<>();
    private WeeklyAvailabilityAdapter adapter;

    private boolean dirty = false;

    private static final ZoneId PROVIDER_ZONE = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sp_availability);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAvailability);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        rvWeekly = findViewById(R.id.rvWeekly);
        btnSave = findViewById(R.id.btnSave);

        setSelectedDate(LocalDate.now(PROVIDER_ZONE));
        findViewById(R.id.cardDate).setOnClickListener(v -> openDatePicker());

        adapter = new WeeklyAvailabilityAdapter(this, weekly, () -> {
            dirty = true;
            btnSave.setEnabled(true);
        });

        rvWeekly.setLayoutManager(new LinearLayoutManager(this));
        rvWeekly.setAdapter(adapter);

        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> saveWeeklyAvailability());

        loadWeeklyAvailability();
    }

    private void setSelectedDate(LocalDate date) {
        tvSelectedDate.setText(date.format(DATE_FMT));
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pick a date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtcMillis -> {
            LocalDate d = Instant.ofEpochMilli(selectionUtcMillis)
                    .atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(PROVIDER_ZONE)
                    .toLocalDate();
            setSelectedDate(d);
            Toast.makeText(this, "Daily override coming next 👀", Toast.LENGTH_SHORT).show();
        });

        picker.show(getSupportFragmentManager(), "datePicker");
    }

    // -----------------------------
    // LOAD WEEKLY (ALWAYS 7 DAYS)
    // -----------------------------
    private void loadWeeklyAvailability() {
        String providerId = TokenStore.getUserId(this);
        if (providerId == null || providerId.isEmpty()) {
            Toast.makeText(this, "No provider session found.", Toast.LENGTH_SHORT).show();
            normalizeWeekly(null);
            return;
        }

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        rest.listWeeklyAvailability(
                "eq." + providerId,
                "id,provider_id,day_of_week,start_time,end_time,enabled",
                "day_of_week.asc"
        ).enqueue(new Callback<List<WeeklyAvailabilityDto>>() {
            @Override
            public void onResponse(Call<List<WeeklyAvailabilityDto>> call, Response<List<WeeklyAvailabilityDto>> resp) {
                if (!resp.isSuccessful()) {
                    Toast.makeText(SPAvailabilityActivity.this, "Load failed: " + resp.code(), Toast.LENGTH_SHORT).show();
                    normalizeWeekly(null);
                    return;
                }

                normalizeWeekly(resp.body());
                dirty = false;
                btnSave.setEnabled(false);
            }

            @Override
            public void onFailure(Call<List<WeeklyAvailabilityDto>> call, Throwable t) {
                Toast.makeText(SPAvailabilityActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                normalizeWeekly(null);
            }
        });
    }

    private void normalizeWeekly(List<WeeklyAvailabilityDto> data) {
        Map<Integer, WeeklyAvailabilityDto> map = new HashMap<>();
        if (data != null) {
            for (WeeklyAvailabilityDto r : data) {
                if (r != null && r.day_of_week >= 0 && r.day_of_week <= 6) {
                    map.put(r.day_of_week, r);
                }
            }
        }

        weekly.clear();
        for (int dow = 0; dow <= 6; dow++) {
            WeeklyAvailabilityDto r = map.get(dow);
            if (r == null) {
                r = new WeeklyAvailabilityDto();
                r.id = 0;
                r.provider_id = null;
                r.day_of_week = dow;
                r.start_time = "09:00:00";
                r.end_time = "17:00:00";
                r.enabled = (dow >= 1 && dow <= 5);
            } else {
                if (r.start_time == null) r.start_time = "09:00:00";
                if (r.end_time == null) r.end_time = "17:00:00";
            }
            weekly.add(r);
        }

        adapter.notifyDataSetChanged();
    }

    // -----------------------------
    // SAVE WEEKLY (UPDATE-THEN-INSERT, 409 SAFE)
    // -----------------------------
    private void saveWeeklyAvailability() {
        String providerId = TokenStore.getUserId(this);

        if (providerId == null || providerId.trim().isEmpty()) {
            Toast.makeText(this, "Missing provider session. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        btnSave.setEnabled(false);
        Toast.makeText(this, "Saving weekly availability...", Toast.LENGTH_SHORT).show();

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);
        upsertRow(rest, providerId, 0);
    }

    private void upsertRow(SupabaseRestService rest, String providerId, int idx) {
        if (idx >= weekly.size()) {
            dirty = false;
            btnSave.setEnabled(false);
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            return;
        }

        WeeklyAvailabilityDto row = weekly.get(idx);

        WeeklyAvailabilityDto body = new WeeklyAvailabilityDto();
        body.provider_id = providerId;
        body.day_of_week = row.day_of_week;
        body.start_time = row.start_time;
        body.end_time = row.end_time;
        body.enabled = row.enabled;

        Log.d(TAG, "UPSERT dow=" + row.day_of_week + " id=" + row.id);

        // 1) Try UPDATE by provider_id + day_of_week
        rest.updateWeeklyAvailabilityByProviderDay(
                "eq." + providerId,
                "eq." + row.day_of_week,
                body
        ).enqueue(new Callback<List<WeeklyAvailabilityDto>>() {
            @Override
            public void onResponse(Call<List<WeeklyAvailabilityDto>> call, Response<List<WeeklyAvailabilityDto>> resp) {
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    weekly.get(idx).id = resp.body().get(0).id;
                    upsertRow(rest, providerId, idx + 1);
                    return;
                }

                // If update affected 0 rows (common if no row exists OR RLS blocks update), then try INSERT
                insertOrHandle409(rest, providerId, idx, body);
            }

            @Override
            public void onFailure(Call<List<WeeklyAvailabilityDto>> call, Throwable t) {
                Toast.makeText(SPAvailabilityActivity.this, "Update failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            }
        });
    }

    private void insertOrHandle409(SupabaseRestService rest, String providerId, int idx, WeeklyAvailabilityDto body) {
        rest.insertWeeklyAvailability(body).enqueue(new Callback<List<WeeklyAvailabilityDto>>() {
            @Override
            public void onResponse(Call<List<WeeklyAvailabilityDto>> call, Response<List<WeeklyAvailabilityDto>> resp) {

                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    weekly.get(idx).id = resp.body().get(0).id;
                    upsertRow(rest, providerId, idx + 1);
                    return;
                }

                // 409 conflict: row already exists -> do one more UPDATE (covers race/rls edge cases)
                if (resp.code() == 409) {
                    Log.w(TAG, "Insert 409 conflict. Retrying update for dow=" + body.day_of_week);

                    rest.updateWeeklyAvailabilityByProviderDay(
                            "eq." + providerId,
                            "eq." + body.day_of_week,
                            body
                    ).enqueue(new Callback<List<WeeklyAvailabilityDto>>() {
                        @Override
                        public void onResponse(Call<List<WeeklyAvailabilityDto>> call2, Response<List<WeeklyAvailabilityDto>> resp2) {
                            if (resp2.isSuccessful()) {
                                upsertRow(rest, providerId, idx + 1);
                            } else {
                                Toast.makeText(SPAvailabilityActivity.this, "Save failed: " + resp2.code(), Toast.LENGTH_LONG).show();
                                btnSave.setEnabled(true);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<WeeklyAvailabilityDto>> call2, Throwable t) {
                            Toast.makeText(SPAvailabilityActivity.this, "Save failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                        }
                    });

                    return;
                }

                Toast.makeText(SPAvailabilityActivity.this, "Insert failed: " + resp.code(), Toast.LENGTH_LONG).show();
                btnSave.setEnabled(true);
            }

            @Override
            public void onFailure(Call<List<WeeklyAvailabilityDto>> call, Throwable t) {
                Toast.makeText(SPAvailabilityActivity.this, "Insert failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            }
        });
    }
}
