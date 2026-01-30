package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.TimeslotsAdapter;
import com.example.selahbookingsystem.util.NailDurationCalculator;

import java.util.Arrays;
import java.util.List;

public class PickTimeslotActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";
    public static final String EXTRA_SELECTED_MAP = "extra_selected_map";
    public static final String EXTRA_EST_MINS = "extra_est_mins";
    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    public static final String EXTRA_SELECTED_SLOT = "extra_selected_slot";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_timeslot);

        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        String currentUri = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        String inspoUri = getIntent().getStringExtra(EXTRA_INSPO_URI);
        String rawMap = getIntent().getStringExtra(EXTRA_SELECTED_MAP);
        int estMins = getIntent().getIntExtra(EXTRA_EST_MINS, 60);
        String serviceId = getIntent().getStringExtra(EXTRA_SERVICE_ID);

        if (TextUtils.isEmpty(providerId) || TextUtils.isEmpty(currentUri) || TextUtils.isEmpty(rawMap)) {
            Toast.makeText(this, "Missing booking info. Go back.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView tvDuration = findViewById(R.id.tvDuration);
        tvDuration.setText("Duration: " + NailDurationCalculator.pretty(estMins));

        RecyclerView rv = findViewById(R.id.rvTimes);
        rv.setLayoutManager(new LinearLayoutManager(this));

        TimeslotsAdapter adapter = new TimeslotsAdapter(slotLabel -> {
            Intent i = new Intent(this, ConfirmBookingActivity.class);
            i.putExtra(ConfirmBookingActivity.EXTRA_PROVIDER_ID, providerId);
            i.putExtra(ConfirmBookingActivity.EXTRA_PROVIDER_NAME, providerName);
            i.putExtra(ConfirmBookingActivity.EXTRA_CURRENT_URI, currentUri);
            if (!TextUtils.isEmpty(inspoUri)) i.putExtra(ConfirmBookingActivity.EXTRA_INSPO_URI, inspoUri);
            i.putExtra(ConfirmBookingActivity.EXTRA_SELECTED_MAP, rawMap);
            i.putExtra(ConfirmBookingActivity.EXTRA_EST_MINS, estMins);
            i.putExtra(ConfirmBookingActivity.EXTRA_SELECTED_SLOT, slotLabel);
            // i.putExtra(ConfirmBookingActivity.EXTRA_SERVICE_ID, serviceId); // keep even if unused now

            startActivity(i);
        });

        rv.setAdapter(adapter);

        // Phase 1 hardcoded slots (your ConfirmBookingActivity supports Today/Tomorrow/Fri formats)
        List<String> slots = Arrays.asList(
                "Today • 14:00",
                "Today • 15:30",
                "Tomorrow • 10:00",
                "Tomorrow • 11:30",
                "Fri • 09:30"
        );
        adapter.submit(slots);
    }
}
