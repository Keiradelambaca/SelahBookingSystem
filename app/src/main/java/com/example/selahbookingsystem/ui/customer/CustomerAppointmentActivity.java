package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.CustomerAppointment;
import com.example.selahbookingsystem.adapter.CustomerAppointmentsAdapter;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Appointments");
        setSupportActionBar(toolbar);

        rvAppointments = findViewById(R.id.rvAppointments);
        emptyState = findViewById(R.id.emptyState);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CustomerAppointmentsAdapter(this, upcomingAppointments, appt -> {
            Intent i = new Intent(this, CustomerAppointmentDetailActivity.class);
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_BOOKING_ID, appt.getId());
            startActivity(i);
        }, false);
        rvAppointments.setAdapter(adapter);

        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
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

        String nowIso = Instant.now().toString();

        String select =
                "id,provider_id,provider_name,start_time,end_time,duration_mins,details_json," +
                        "inspo_photo_url,current_photo_url,status,created_at,total_price_cents," +
                        "payment_status";

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
                        continue;
                    }

                    upcomingAppointments.add(appt);
                }

                pruneSortAndRender();
            }

            @Override
            public void onFailure(retrofit2.Call<List<BookingDto>> call, Throwable t) {
                Log.e(TAG, "API failure", t);
                pruneSortAndRender();
            }
        });
    }

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

        boolean isEmpty = upcomingAppointments.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvAppointments.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        adapter.notifyDataSetChanged();
    }

    private CustomerAppointment mapBookingToAppointment(BookingDto b) {
        CustomerAppointment a = new CustomerAppointment();

        a.setId(b.id);
        a.setProviderName(
                b.provider_name != null && !b.provider_name.trim().isEmpty()
                        ? b.provider_name
                        : "Provider"
        );

        a.setAppointmentStart(parseToInstant(b.start_time));
        a.setDurationMins(b.duration_mins != null ? b.duration_mins : 60);
        a.setServiceTitle("Nail Appointment");

        String banner =
                (b.inspo_photo_url != null && !b.inspo_photo_url.trim().isEmpty())
                        ? b.inspo_photo_url
                        : b.current_photo_url;
        a.setBannerUrl(banner);

        a.setLocationArea("Near you");

        double price = 0.0;
        if (b.total_price_cents != null) {
            price = b.total_price_cents / 100.0;
        }
        a.setPrice(price);

        a.setPaymentStatus(mapPaymentStatus(b.payment_status, b.status));

        return a;
    }

    private CustomerAppointment.PaymentStatus mapPaymentStatus(String paymentStatus, String fallbackStatus) {
        String value = paymentStatus;

        if (value == null || value.trim().isEmpty()) {
            value = fallbackStatus;
        }

        if (value == null) {
            return CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID;
        }

        value = value.trim().toLowerCase();

        if (value.equals("paid")) {
            return CustomerAppointment.PaymentStatus.PAID;
        }

        if (value.equals("deposit_paid")) {
            return CustomerAppointment.PaymentStatus.DEPOSIT_PAID;
        }

        if (value.equals("cash") || value.equals("pay_by_cash")) {
            return CustomerAppointment.PaymentStatus.PAY_BY_CASH;
        }

        return CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID;
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