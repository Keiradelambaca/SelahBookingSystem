package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.SPAppointmentCardAdapter;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPHomeActivity extends SPBaseActivity {

    private static final String TAG = "SPHomeActivity";

    private TextView tvGreeting;
    private TextView tvEmptyState;
    private RecyclerView rvAppointments;

    private final List<BookingDto> appointmentItems = new ArrayList<>();
    private SPAppointmentCardAdapter adapter;
    private SupabaseRestService rest;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_sp_home;
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_home;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvAppointments = findViewById(R.id.rvAppointments);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SPAppointmentCardAdapter(this, appointmentItems);
        rvAppointments.setAdapter(adapter);

        ApiClient.init(getApplicationContext());
        rest = ApiClient.get().create(SupabaseRestService.class);

        loadProviderName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void loadProviderName() {
        String userId = TokenStore.getUserId(this);

        if (isBlank(userId)) {
            tvGreeting.setText("Hello Service Provider,");
            return;
        }

        rest.getProfile("eq." + userId, "id,full_name,business_name")
                .enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
                    @Override
                    public void onResponse(Call<List<SupabaseRestService.ProfileDto>> call,
                                           Response<List<SupabaseRestService.ProfileDto>> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            tvGreeting.setText("Hello Service Provider,");
                            return;
                        }

                        SupabaseRestService.ProfileDto profile = response.body().get(0);
                        String displayName = firstNonBlank(
                                profile.business_name,
                                profile.full_name,
                                "Service Provider"
                        );
                        tvGreeting.setText("Hello " + displayName + ",");
                    }

                    @Override
                    public void onFailure(Call<List<SupabaseRestService.ProfileDto>> call, Throwable t) {
                        tvGreeting.setText("Hello Service Provider,");
                    }
                });
    }

    private void loadAppointments() {
        String providerId = TokenStore.getUserId(this);

        if (isBlank(providerId)) {
            Log.e(TAG, "Provider ID is blank");
            appointmentItems.clear();
            adapter.notifyDataSetChanged();
            showEmptyState(true);
            return;
        }

        Log.d(TAG, "Loading appointments for providerId = " + providerId);

        String select =
                "id,client_id,provider_id,status,start_time,end_time,created_at," +
                        "duration_mins,inspo_photo_url,provider_name," +
                        "client:profiles!bookings_client_id_fkey(id,full_name,email,phone)";

        rest.listConfirmedBookingsWithClient(
                "eq." + providerId,
                "not.is.null",
                select,
                "start_time.asc"
        ).enqueue(new Callback<List<SupabaseRestService.BookingWithClientDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.BookingWithClientDto>> call,
                                   Response<List<SupabaseRestService.BookingWithClientDto>> response) {

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to load appointments: code=" + response.code());

                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body", e);
                    }

                    appointmentItems.clear();
                    adapter.notifyDataSetChanged();
                    showEmptyState(true);
                    return;
                }

                List<SupabaseRestService.BookingWithClientDto> body = response.body();
                Log.d(TAG, "Raw bookings returned = " + (body == null ? 0 : body.size()));

                if (body == null || body.isEmpty()) {
                    appointmentItems.clear();
                    adapter.notifyDataSetChanged();
                    showEmptyState(true);
                    return;
                }

                List<BookingDto> mapped = new ArrayList<>();

                for (SupabaseRestService.BookingWithClientDto row : body) {
                    Log.d(TAG,
                            "Booking row -> id=" + row.id +
                                    ", provider_id=" + row.provider_id +
                                    ", status=" + row.status +
                                    ", start_time=" + row.start_time +
                                    ", end_time=" + row.end_time
                    );

                    BookingDto dto = new BookingDto();
                    dto.id = row.id;
                    dto.client_id = row.client_id;
                    dto.provider_id = row.provider_id;
                    dto.status = row.status;
                    dto.start_time = row.start_time;
                    dto.end_time = row.end_time;
                    dto.created_at = row.created_at;
                    dto.duration_mins = row.duration_mins;
                    dto.inspo_photo_url = row.inspo_photo_url;
                    dto.provider_name = row.provider_name;

                    if (row.client != null) {
                        dto.client_name = row.client.full_name;
                    }

                    mapped.add(dto);
                }

                List<BookingDto> filtered = filterConfirmedNext7Days(mapped);

                Log.d(TAG, "Filtered bookings for home = " + filtered.size());

                appointmentItems.clear();
                appointmentItems.addAll(filtered);
                adapter.notifyDataSetChanged();

                showEmptyState(appointmentItems.isEmpty());
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.BookingWithClientDto>> call, Throwable t) {
                Log.e(TAG, "loadAppointments onFailure", t);
                appointmentItems.clear();
                adapter.notifyDataSetChanged();
                showEmptyState(true);
            }
        });
    }

    private void showEmptyState(boolean show) {
        tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvAppointments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "Service Provider";

        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }

        return "Service Provider";
    }

    private List<BookingDto> filterConfirmedNext7Days(List<BookingDto> source) {
        List<BookingDto> result = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = now.plusDays(7);

        for (BookingDto booking : source) {
            try {
                if (booking.start_time == null || booking.end_time == null) {
                    Log.d(TAG, "Skipping booking with missing dates: " + booking.id);
                    continue;
                }

                OffsetDateTime start = OffsetDateTime.parse(booking.start_time);
                OffsetDateTime bookingEnd = OffsetDateTime.parse(booking.end_time);

                boolean isConfirmed = booking.status != null &&
                        booking.status.trim().equalsIgnoreCase("confirmed");

                boolean startsWithinNext7Days =
                        (start.isEqual(now) || start.isAfter(now)) && start.isBefore(end);

                boolean notAlreadyFinished = bookingEnd.isAfter(now);

                Log.d(TAG,
                        "Filter check -> id=" + booking.id +
                                ", status=" + booking.status +
                                ", start=" + booking.start_time +
                                ", confirmed=" + isConfirmed +
                                ", startsWithinNext7Days=" + startsWithinNext7Days +
                                ", notAlreadyFinished=" + notAlreadyFinished
                );

                if (isConfirmed && startsWithinNext7Days && notAlreadyFinished) {
                    result.add(booking);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse booking date for booking id=" + booking.id, e);
            }
        }

        result.sort((a, b) -> {
            try {
                OffsetDateTime aStart = OffsetDateTime.parse(a.start_time);
                OffsetDateTime bStart = OffsetDateTime.parse(b.start_time);
                return aStart.compareTo(bStart);
            } catch (Exception e) {
                return 0;
            }
        });

        return result;
    }
}