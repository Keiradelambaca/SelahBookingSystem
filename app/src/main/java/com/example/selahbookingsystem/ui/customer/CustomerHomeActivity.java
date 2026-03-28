package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

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

        titleText.setText("Hey Customer,");

        appointmentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAppointmentsAdapter(
                this,
                upcoming,
                appt -> {
                    Intent i = new Intent(this, CustomerAppointmentDetailActivity.class);
                    i.putExtra(CustomerAppointmentDetailActivity.EXTRA_BOOKING_ID, appt.getId());
                    startActivity(i);
                },
                true
        );
        appointmentsRecycler.setAdapter(adapter);

        bookButton.setOnClickListener(v -> {
            Intent i = new Intent(this, SelectProviderActivity.class);
            pickProviderLauncher.launch(i);
        });

        loadCustomerName();
        loadUpcomingPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerName();
        loadUpcomingPreview();
    }

    private void loadCustomerName() {
        String userId = TokenStore.getUserId(this);

        if (TextUtils.isEmpty(userId)) {
            titleText.setText("Hey Customer,");
            return;
        }

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        rest.getProfile(
                "eq." + userId,
                "id,full_name"
        ).enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.ProfileDto>> call,
                                   Response<List<SupabaseRestService.ProfileDto>> response) {

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    titleText.setText("Hey Customer,");
                    return;
                }

                SupabaseRestService.ProfileDto profile = response.body().get(0);

                String fullName = profile.full_name;
                if (TextUtils.isEmpty(fullName)) {
                    titleText.setText("Hey Customer,");
                    return;
                }

                String firstName = extractFirstName(fullName);
                titleText.setText("Hey " + firstName + ",");
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.ProfileDto>> call, Throwable t) {
                titleText.setText("Hey Customer,");
            }
        });
    }

    private String extractFirstName(String fullName) {
        if (fullName == null) return "Customer";

        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) return "Customer";

        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace == -1) {
            return trimmed;
        }

        return trimmed.substring(0, firstSpace);
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
                        "details_json,inspo_photo_url,current_photo_url,status,created_at," +
                        "total_price_cents,payment_status";

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
                        CustomerAppointment mapped = mapBookingToAppointment(b);
                        if (mapped.getAppointmentStart() != null) {
                            upcoming.add(mapped);
                        }
                    }
                }

                pruneAndSortUpcoming();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<BookingDto>> call, Throwable t) {
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

        a.setProviderName(
                !TextUtils.isEmpty(b.provider_name)
                        ? b.provider_name
                        : "Provider"
        );

        a.setAppointmentStart(parseToInstant(b.start_time));
        a.setDurationMins(b.duration_mins != null ? b.duration_mins : 60);
        a.setServiceTitle("Nail Appointment");

        String banner =
                !TextUtils.isEmpty(b.inspo_photo_url)
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

        if (TextUtils.isEmpty(value)) {
            value = fallbackStatus;
        }

        if (TextUtils.isEmpty(value)) {
            return CustomerAppointment.PaymentStatus.DEPOSIT_NOT_PAID;
        }

        value = value.trim().toLowerCase();

        if ("paid".equals(value)) {
            return CustomerAppointment.PaymentStatus.PAID;
        }

        if ("deposit_paid".equals(value)) {
            return CustomerAppointment.PaymentStatus.DEPOSIT_PAID;
        }

        if ("cash".equals(value) || "pay_by_cash".equals(value)) {
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

    private final androidx.activity.result.ActivityResultLauncher<Intent> pickProviderLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
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