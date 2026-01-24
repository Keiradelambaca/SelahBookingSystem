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
        pruneSortAndRender();
    }

    /**
     * Load appointments from backend (Supabase/Firebase later).
     * Currently mocked so UI can be built safely.
     */
    private void loadAppointments() {
        List<CustomerAppointment> all = fetchAppointmentsMock();

        upcomingAppointments.clear();
        upcomingAppointments.addAll(all);

        pruneSortAndRender();
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


    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_appointments;
    }

}