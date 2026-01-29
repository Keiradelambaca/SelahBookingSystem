package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.CustomerAppointment;
import com.example.selahbookingsystem.adapter.CustomerAppointmentsAdapter;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomerAppointmentActivity extends BaseActivity {

    private RecyclerView rvAppointments;
    private View emptyState;

    private final List<CustomerAppointment> upcomingAppointments = new ArrayList<>();
    private CustomerAppointmentsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_appointments);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Appointments");

        rvAppointments = findViewById(R.id.rvAppointments);
        emptyState = findViewById(R.id.emptyState);

        // Recycler setup
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAppointmentsAdapter(this, upcomingAppointments, appt -> {

            Intent i = new Intent(this, CustomerAppointmentDetailActivity.class);
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_APPT_ID, appt.getId());
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_TITLE, appt.getServiceTitle());
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_PROVIDER, appt.getProviderName());
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_LOCATION_AREA, appt.getLocationArea());
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_PRICE, appt.getPrice());
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_PAYMENT_STATUS, getPaymentText(appt));
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_BANNER_URL, appt.getBannerUrl());

            // Temporary location (replace later with provider coords from backend)
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_LAT, 53.3498);
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_LNG, -6.2603);

            // If you already format date elsewhere you can pass it here
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_DATETIME, "");

            startActivity(i);
        });

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
        if (clientId == null || clientId.isEmpty()) {
            upcomingAppointments.clear();
            pruneSortAndRender();
            return;
        }

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        String nowIso = Instant.now().toString();

        rest.listUpcomingBookingsForClient(
                "eq." + clientId,
                "gte." + nowIso,
                "start_time.asc",
                "id,provider_id,provider_name,start_time,end_time,duration_mins,details_json,inspo_photo_url,current_photo_url,staus,created_at"
        ).enqueue(new retrofit2.Callback<List<SupabaseRestService.BookingDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<SupabaseRestService.BookingDto>> call,
                                   retrofit2.Response<List<SupabaseRestService.BookingDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    upcomingAppointments.clear();
                    pruneSortAndRender();
                    return;
                }

                upcomingAppointments.clear();
                for (SupabaseRestService.BookingDto b : resp.body()) {
                    upcomingAppointments.add(mapBookingToAppointment(b));
                }

                pruneSortAndRender();
            }

            @Override
            public void onFailure(retrofit2.Call<List<SupabaseRestService.BookingDto>> call, Throwable t) {
                // Optional: show a toast
                pruneSortAndRender();
            }
        });
    }


    /**
     * Removes past appointments and sorts upcoming by nearest date.
     */
    private void pruneSortAndRender() {
        Instant now = Instant.now();

        List<CustomerAppointment> filtered = new ArrayList<>();
        for (CustomerAppointment a : upcomingAppointments) {
            if (a.getAppointmentStart() != null &&
                    a.getAppointmentStart().isAfter(now)) {
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

    /**
     * Temporary fake data so UI works while backend is not wired.
     * Delete once Supabase is connected.
     */
    private List<CustomerAppointment> fetchAppointmentsMock() {
        List<CustomerAppointment> list = new ArrayList<>();

        list.add(MockAppointments.createInHours(4));
        list.add(MockAppointments.createInDays(1));
        list.add(MockAppointments.createInDays(3));

        return list;
    }

    private String getPaymentText(CustomerAppointment appt) {
        switch (appt.getPaymentStatus()) {
            case DEPOSIT_PAID:
                return "Deposit paid";
            case DEPOSIT_NOT_PAID:
                return "Deposit not paid";
            case PAID:
                return "Paid";
            case PAY_BY_CASH:
                return "Pay by cash";
            default:
                return "";
        }
    }

    private CustomerAppointment mapBookingToAppointment(SupabaseRestService.BookingDto b) {
        // Convert ISO start_time to Instant
        Instant start = null;
        try { start = Instant.parse(b.start_time); } catch (Exception ignored) {}

        // Create your CustomerAppointment
        CustomerAppointment a = new CustomerAppointment();

        a.setId(b.id);
        a.setProviderName(b.provider_name != null ? b.provider_name : "Provider");
        a.setAppointmentStart(start);

        // Use duration or end_time if your model supports it
        a.setDurationMins(b.duration_mins != null ? b.duration_mins : 60);

        // If you have a title field like “Full set • Long • French”
        a.setServiceTitle("Nail Appointment");

        // Optional: you can set banner to inspo image
        a.setBannerUrl(b.inspo_photo_url);

        // Optional placeholders if your detail screen expects these
        a.setLocationArea("Near you");

        // Payment status can stay mocked for now
        // a.setPaymentStatus(...)

        return a;
    }



    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_appointments;
    }

}