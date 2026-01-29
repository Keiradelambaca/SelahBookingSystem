package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.AppointmentsAdapter;
import com.example.selahbookingsystem.data.store.RoleStore;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerHomeActivity extends com.example.selahbookingsystem.ui.base.BaseActivity {

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_home;
    }

    private AppointmentsAdapter apptAdapter;
    private SupabaseRestService rest;

    private final ActivityResultLauncher<Intent> pickProviderLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String providerId = result.getData()
                                    .getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_ID);
                            String providerName = result.getData()
                                    .getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_NAME);

                            if (providerId == null || providerId.isEmpty()) {
                                Toast.makeText(this, "No provider selected", Toast.LENGTH_LONG).show();
                                return;
                            }

                            Intent next = new Intent(this, BookingPhotosActivity.class);
                            next.putExtra(BookingPhotosActivity.EXTRA_PROVIDER_ID, providerId);
                            next.putExtra(BookingPhotosActivity.EXTRA_PROVIDER_NAME, providerName);
                            startActivity(next);
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_customer_home);

        rest = ApiClient.get().create(SupabaseRestService.class);

        TextView titleText = findViewById(R.id.titleText);
        Button bookBtn = findViewById(R.id.bookButton);

        String email = getIntent().getStringExtra("email");
        if (email == null) email = "";
        String name = RoleStore.getName(this, email);
        titleText.setText("Hey " + (name != null ? name : "Customer") + ",");

        RecyclerView apptRv = findViewById(R.id.appointmentsRecycler);
        apptRv.setLayoutManager(new LinearLayoutManager(this));
        apptAdapter = new AppointmentsAdapter();
        apptRv.setAdapter(apptAdapter);

        bookBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, SelectProviderActivity.class);
            pickProviderLauncher.launch(i);
        });

        // Load real upcoming bookings (preview)
        loadUpcomingForHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUpcomingForHome(); // refresh when returning from booking flow
    }

    private void loadUpcomingForHome() {

        String clientId = TokenStore.getUserId(this);
        if (clientId == null || clientId.isEmpty()) {
            apptAdapter.submit(new ArrayList<>());
            return;
        }

        String nowIso = Instant.now().toString();

        rest.listUpcomingBookingsForClientLimited(
                "eq." + clientId,
                "gte." + nowIso,
                "start_time.asc",
                // Your table columns:
                "id,provider_name,start_time",
                3
        ).enqueue(new Callback<List<SupabaseRestService.BookingDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.BookingDto>> call,
                                   Response<List<SupabaseRestService.BookingDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    apptAdapter.submit(new ArrayList<>());
                    return;
                }

                List<AppointmentsAdapter.Item> items = new ArrayList<>();
                for (SupabaseRestService.BookingDto b : resp.body()) {
                    String provider = (b.provider_name != null && !b.provider_name.isEmpty())
                            ? b.provider_name
                            : "Provider";

                    String when = formatPretty(b.start_time);

                    items.add(new AppointmentsAdapter.Item(
                            "Booking with " + provider,
                            when
                    ));
                }

                apptAdapter.submit(items);
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.BookingDto>> call, Throwable t) {
                apptAdapter.submit(new ArrayList<>());
            }
        });
    }

    private String formatPretty(String iso) {
        try {
            Instant inst = Instant.parse(iso);
            ZonedDateTime z = inst.atZone(ZoneId.systemDefault());
            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm");
            return z.format(f);
        } catch (Exception e) {
            return iso != null ? iso : "";
        }
    }
}
