package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.CustomerAppointment;
import com.example.selahbookingsystem.adapter.CustomerAppointmentsAdapter;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.ui.base.BaseActivity;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerHomeActivity extends BaseActivity {

    private TextView titleText;
    private Button bookButton;

    private RecyclerView appointmentsRecycler;

    private final List<CustomerAppointment> upcoming = new ArrayList<>();
    private CustomerAppointmentsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        titleText = findViewById(R.id.titleText);
        bookButton = findViewById(R.id.bookButton);
        appointmentsRecycler = findViewById(R.id.appointmentsRecycler);

        // Optional: personalize greeting if name is elsewhere
        titleText.setText("Hey Customer,");

        // Recycler setup
        appointmentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAppointmentsAdapter(this, upcoming, appt -> {
            Intent i = new Intent(this, CustomerAppointmentDetailActivity.class);
            i.putExtra(CustomerAppointmentDetailActivity.EXTRA_BOOKING_ID, appt.getId());
            startActivity(i);
        });
        appointmentsRecycler.setAdapter(adapter);

        // Book button -> booking flow screen
        bookButton.setOnClickListener(v -> {
            Intent i = new Intent(this, SelectProviderActivity.class);
            pickProviderLauncher.launch(i);
        });


        loadUpcomingPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUpcomingPreview();
    }

    private void loadUpcomingPreview() {
        String clientId = TokenStore.getUserId(this);

        if (clientId == null || clientId.isEmpty()) {
            upcoming.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        String nowIso = Instant.now().toString();

        String select =
                "id,provider_id,provider_name,start_time,end_time,duration_mins," +
                        "details_json,inspo_photo_url,current_photo_url,status,created_at";

        // show next 3 on home
        rest.listUpcomingBookingsForClientLimited(
                "eq." + clientId,
                "gte." + nowIso,
                "start_time.asc",
                select,
                3
        ).enqueue(new Callback<List<BookingDto>>() {
            @Override
            public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> resp) {
                upcoming.clear();

                if (resp.isSuccessful() && resp.body() != null) {
                    for (BookingDto b : resp.body()) {
                        upcoming.add(mapBookingToAppointment(b));
                    }
                }

                pruneAndSortUpcoming();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                // keep list empty / show whatever last loaded
                upcoming.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void pruneAndSortUpcoming() {
        Instant now = Instant.now();
        upcoming.removeIf(a -> a.getAppointmentStart() == null || !a.getAppointmentStart().isAfter(now));
        upcoming.sort(Comparator.comparing(CustomerAppointment::getAppointmentStart));
    }

    private CustomerAppointment mapBookingToAppointment(BookingDto b) {
        CustomerAppointment a = new CustomerAppointment();

        a.setId(b.id);
        a.setProviderName(b.provider_name != null ? b.provider_name : "Provider");
        a.setAppointmentStart(parseToInstant(b.start_time));

        a.setDurationMins(b.duration_mins != null ? b.duration_mins : 60);

        // no service title column yet
        a.setServiceTitle("Nail Appointment");

        // Use inspo image first; fallback to current photo
        String banner =
                (b.inspo_photo_url != null && !b.inspo_photo_url.isEmpty())
                        ? b.inspo_photo_url
                        : b.current_photo_url;

        a.setBannerUrl(banner);

        a.setLocationArea("Near you");

        a.setPaymentStatus(mapPaymentStatus(b.status));

        // price isn’t in booking columns yet
        a.setPrice(0);

        return a;
    }

    private CustomerAppointment.PaymentStatus mapPaymentStatus(String status) {
        if (status == null) return CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID;

        if ("paid".equalsIgnoreCase(status)) return CustomerAppointment.PaymentStatus.PAID;
        if ("deposit_paid".equalsIgnoreCase(status)) return CustomerAppointment.PaymentStatus.DEPOSIT_PAID;
        if ("cash".equalsIgnoreCase(status) || "pay_by_cash".equalsIgnoreCase(status))
            return CustomerAppointment.PaymentStatus.PAY_BY_CASH;

        return CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID;
    }

    private Instant parseToInstant(String iso) {
        if (iso == null) return null;

        // Handles "2026-01-29T14:30:00Z"
        try { return Instant.parse(iso); } catch (Exception ignored) {}

        // Handles "2026-01-29T14:30:00+00:00"
        try { return OffsetDateTime.parse(iso).toInstant(); } catch (Exception ignored) {}

        return null;
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> pickProviderLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;

                        String providerId = result.getData().getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_ID);
                        String providerName = result.getData().getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_NAME);

                        Intent next = new Intent(this, BookingPhotosActivity.class);
                        next.putExtra(BookingPhotosActivity.EXTRA_PROVIDER_ID, providerId);
                        next.putExtra(BookingPhotosActivity.EXTRA_PROVIDER_NAME, providerName);
                        startActivity(next);
                    });


    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_home;
    }
}

