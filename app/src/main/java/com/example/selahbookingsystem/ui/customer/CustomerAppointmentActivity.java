package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.CustomerAppointment;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.adapter.CustomerAppointmentsAdapter;
import com.example.selahbookingsystem.ui.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomerAppointmentActivity extends BaseActivity {

    private static final String TAG = "APPTS";

    private RecyclerView rvAppointments;
    private View emptyState;

    private final List<CustomerAppointment> upcomingAppointments = new ArrayList<>();
    private CustomerAppointmentsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_appointments);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarAppointments);
        // If you set app:title in XML you can remove the next line
        toolbar.setTitle("Appointments");
        setSupportActionBar(toolbar);

        rvAppointments = findViewById(R.id.rvAppointments);
        emptyState = findViewById(R.id.emptyState);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CustomerAppointmentsAdapter(this, upcomingAppointments, appt -> {
            Intent i = new Intent(this, CustomerAppointmentDetailActivity.class);
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_BOOKING_ID, appt.getId());
            startActivity(i);
        });
        rvAppointments.setAdapter(adapter);

        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments(); // refresh after changes/cancel etc.
    }

    private void loadAppointments() {
        String clientId = TokenStore.getUserId(this);
        if (clientId == null || clientId.trim().isEmpty()) {
            Log.w(TAG, "No clientId found - clearing list");
            upcomingAppointments.clear();
            pruneSortAndRender();
            return;
        }

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        // ISO UTC like "2026-01-30T16:23:00Z"
        String nowIso = Instant.now().toString();

        String select =
                "id,provider_id,provider_name,start_time,end_time,duration_mins,details_json," +
                        "inspo_photo_url,current_photo_url,status,created_at";

        Log.d(TAG, "Requesting bookings client=" + clientId + " start_time>= " + nowIso);

        rest.listUpcomingBookingsForClient(
                "eq." + clientId,
                "gte." + nowIso,
                "start_time.asc",
                select,
                200
        ).enqueue(new retrofit2.Callback<List<BookingDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<BookingDto>> call,
                                   retrofit2.Response<List<BookingDto>> resp) {

                int rows = (resp.body() == null) ? 0 : resp.body().size();
                Log.d(TAG, "API response ok=" + resp.isSuccessful()
                        + " code=" + resp.code()
                        + " rows=" + rows);

                if (!resp.isSuccessful() || resp.body() == null) {
                    upcomingAppointments.clear();
                    pruneSortAndRender();
                    return;
                }

                upcomingAppointments.clear();

                for (BookingDto b : resp.body()) {
                    CustomerAppointment appt = mapBookingToAppointment(b);

                    if (appt.getAppointmentStart() == null) {
                        Log.w(TAG, "NULL start_time for booking id=" + b.id + " raw=" + b.start_time);
                        continue; // skip invalid
                    }

                    upcomingAppointments.add(appt);
                }

                pruneSortAndRender();
            }

            @Override
            public void onFailure(retrofit2.Call<List<BookingDto>> call, Throwable t) {
                Log.e(TAG, "API failure", t);
                // keep existing list, but refresh empty state + notify
                pruneSortAndRender();
            }
        });
    }

    /**
     * Keep ONLY future appointments, sort ascending, render.
     * This is a safety net even though the API already asks for start_time >= now.
     */
    private void pruneSortAndRender() {
        Instant now = Instant.now();

        List<CustomerAppointment> filtered = new ArrayList<>();
        for (CustomerAppointment a : upcomingAppointments) {
            Instant s = a.getAppointmentStart();
            if (s != null && s.isAfter(now)) {
                filtered.add(a);
            }
        }

        filtered.sort(Comparator.comparing(CustomerAppointment::getAppointmentStart));

        upcomingAppointments.clear();
        upcomingAppointments.addAll(filtered);

        Log.d(TAG, "After prune count=" + upcomingAppointments.size());

        boolean isEmpty = upcomingAppointments.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvAppointments.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        adapter.notifyDataSetChanged();
    }

    private CustomerAppointment mapBookingToAppointment(BookingDto b) {
        Instant start = parseToInstant(b.start_time);

        CustomerAppointment a = new CustomerAppointment();
        a.setId(b.id);
        a.setProviderName(b.provider_name != null ? b.provider_name : "Provider");
        a.setAppointmentStart(start);
        a.setDurationMins(b.duration_mins != null ? b.duration_mins : 60);

        // You can improve this later by pulling service name from details_json
        a.setServiceTitle("Nail Appointment");

        a.setBannerUrl(b.inspo_photo_url);
        a.setLocationArea("Near you");

        // Payment/status mapping (adjust to your real statuses)
        if ("paid".equalsIgnoreCase(b.status)) {
            a.setPaymentStatus(CustomerAppointment.PaymentStatus.PAID);
        } else if ("deposit_paid".equalsIgnoreCase(b.status)) {
            a.setPaymentStatus(CustomerAppointment.PaymentStatus.DEPOSIT_PAID);
        } else if ("cash".equalsIgnoreCase(b.status)) {
            a.setPaymentStatus(CustomerAppointment.PaymentStatus.PAY_BY_CASH);
        } else {
            a.setPaymentStatus(CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID);
        }

        return a;
    }

    private Instant parseToInstant(String iso) {
        if (iso == null) return null;
        try {
            return Instant.parse(iso);
        } catch (Exception ignored) { }
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (Exception ignored) { }
        return null;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_appointments;
    }
}